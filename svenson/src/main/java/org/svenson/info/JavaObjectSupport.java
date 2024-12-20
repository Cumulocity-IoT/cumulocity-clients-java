package org.svenson.info;

import org.svenson.*;

import jakarta.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class JavaObjectSupport extends AbstractObjectSupport
{
    private static final String ADDER_PREFIX = "add";

    private static final String SETTER_PREFIX = "set";

    private static final String GETTER_PREFIX = "get";

    private static final String ISSER_PREFIX = "is";


    public JavaObjectSupport()
    {
    }


    public JSONClassInfo createClassInfo(Class<?> cls)
    {
        Map<String, JavaObjectPropertyInfo> javaNameToInfo = new HashMap<String, JavaObjectPropertyInfo>();

        Constructor<?> ctor = null;
        Class<?> ctorTypeHint = null;
        boolean isWildCard = false;
        int wildCardIndex = -1;
        Method postConstructMethod = null;

        for (Constructor<?> c : cls.getConstructors())
        {
            Annotation[][] parameterAnnotations = c.getParameterAnnotations();

            for (int i = 0, parameterAnnotationsLength = parameterAnnotations.length; i < parameterAnnotationsLength;
                 i++)
            {
                Annotation[] annotations = parameterAnnotations[i];
                for (Annotation annotation : annotations)
                {
                    if (annotation instanceof JSONParameter)
                    {
                        ctor = c;
                    }
                    else if (annotation instanceof JSONParameters)
                    {
                        if (!Map.class.isAssignableFrom(c.getParameterTypes()[i]))
                        {
                            throw new IllegalStateException("@JSONParameters annotation must be on a constructor map parameter");
                        }
                        ctor = c;
                        isWildCard = true;
                        wildCardIndex = i;
                    }
                    if (annotation instanceof JSONTypeHint)
                    {
                        Class<?>[] parameterTypes = c.getParameterTypes();
                        if (wildCardIndex == i)
                        {
                            if (parameterTypes.length != 1)
                            {
                                throw new IllegalStateException("@JSONParameters/@JSONTypeHint combination must only have one map parameter");
                            }
                            if (!Map.class.isAssignableFrom(parameterTypes[0]))
                            {
                                throw new IllegalStateException("@JSONParameters/@JSONTypeHint combination must be on a constructor map parameter");
                            }
                            ctorTypeHint = ((JSONTypeHint)annotation).value();
                        }
                    }
                }
            }
        }

        for (Method m : cls.getMethods())
        {
            String name = m.getName();

            if ((m.getModifiers() & Modifier.PUBLIC) == 0 || name.equals("getClass"))
            {
                continue;
            }

            // patch to skip bridge methods generated for generic classes
            // e.g. ComplexEvent<T>#getPayload
            if (m.isBridge())
            {
                continue;
            }

            if (m.getAnnotation(PostConstruct.class) != null)
            {
                if (m.getParameterTypes().length != 0)
                {
                    throw new IllegalStateException("@PostConstruct methods can't have parameters: " + m);
                }

                postConstructMethod = m;
            }


            if (name.startsWith(SETTER_PREFIX) && m.getParameterTypes().length == 1)
            {
                JSONProperty jsonProperty;
                if (ctor != null && ((jsonProperty = m.getAnnotation(JSONProperty.class)) == null || !jsonProperty.ignore()) )
                {
                    throw new IllegalStateException("Classes with @JSONParameter constructors can't have setters.");
                }

                String javaPropertyName = propertyName(name, SETTER_PREFIX.length());
                JavaObjectPropertyInfo pair = javaNameToInfo.get(javaPropertyName);
                if (pair != null)
                {
                    Method existing = pair.getSetterMethod();

                    if (existing == null || isOverriding(m, existing) || isBestMatchSetter(pair, m))
                    {
                        pair.setSetterMethod(m);
                        pair.setAdderMethod(null);
                    }

                }
                else
                {
                    pair = new JavaObjectPropertyInfo(javaPropertyName, null, m);
                    javaNameToInfo.put(javaPropertyName, pair);
                }

                Class<?>[] parameterTypes;
                Class<?> paramType;
                if ((parameterTypes = m.getParameterTypes()).length == 1 &&
                    (paramType = parameterTypes[0]).isArray())
                {
                    pair.setTypeHint(paramType.getComponentType());
                }

            }
            else if (m.getParameterTypes().length == 0 && !m.getReturnType().equals(void.class))
            {
                if (name.startsWith(GETTER_PREFIX))
                {
                    String javaPropertyName = propertyName(name, GETTER_PREFIX.length());
                    JavaObjectPropertyInfo pair = javaNameToInfo.get(javaPropertyName);
                    if (pair != null)
                    {
                        Method existing = pair.getGetterMethod();

                        if (existing == null || isOverriding(m, existing))
                        {
                            pair.setGetterMethod(m);
                        }
                    }
                    else
                    {
                        javaNameToInfo.put(javaPropertyName, new JavaObjectPropertyInfo(javaPropertyName,
                            m, null));
                    }
                }
                else if (name.startsWith(ISSER_PREFIX))
                {
                    String javaPropertyName = propertyName(name, ISSER_PREFIX.length());
                    JavaObjectPropertyInfo pair = javaNameToInfo.get(javaPropertyName);
                    if (pair != null)
                    {
                        pair.setGetterMethod(m);
                    }
                    else
                    {
                        javaNameToInfo.put(javaPropertyName, new JavaObjectPropertyInfo(javaPropertyName,
                            m, null));
                    }
                }
            }
            else if (name.startsWith(ADDER_PREFIX) && m.getParameterTypes().length == 1)
            {
                JSONProperty jsonProperty;
                if (ctor != null && ((jsonProperty = m.getAnnotation(JSONProperty.class)) == null || !jsonProperty.ignore()) )
                {
                    throw new IllegalStateException("Classes with @JSONParameter constructors can't have adders.");
                }

                String javaPropertyName = propertyName(name, ADDER_PREFIX.length());
                JavaObjectPropertyInfo pair = javaNameToInfo.get(javaPropertyName);
                if (pair != null)
                {
                    if ( pair.getSetterMethod() == null)
                    {
                        pair.setAdderMethod(m);
                    }
                }
                else
                {
                    JavaObjectPropertyInfo newInfo = new JavaObjectPropertyInfo(javaPropertyName, null, null);
                    newInfo.setAdderMethod(m);
                    javaNameToInfo.put(javaPropertyName, newInfo);
                }
            }
        }


        HashMap<String, JavaObjectPropertyInfo> propertyInfos = new HashMap<String, JavaObjectPropertyInfo>(javaNameToInfo.size());

        for (Map.Entry<String, JavaObjectPropertyInfo> e : javaNameToInfo.entrySet())
        {
            String jsonPropertyName = e.getKey();
            JavaObjectPropertyInfo propertyInfo = e.getValue();

            Method getterMethod = propertyInfo.getGetterMethod();
            Method setterMethod = propertyInfo.getSetterMethod();
            Method adderMethod = propertyInfo.getAdderMethod();

            JSONProperty jsonProperty = MethodUtil.getAnnotation(JSONProperty.class, getterMethod,
                setterMethod);

            if (jsonProperty != null)
            {

                if (jsonProperty.value().length() > 0)
                {
                    jsonPropertyName = jsonProperty.value();
                }

                propertyInfo.setIgnore(jsonProperty.ignore());
                propertyInfo.setIgnoreIfNull(jsonProperty.ignoreIfNull());
                propertyInfo.setReadOnly(jsonProperty.readOnly());
                propertyInfo.setPriority(jsonProperty.priority());
            }
            propertyInfo.setJsonName(jsonPropertyName);

            JSONReference refAnno = MethodUtil.getAnnotation(JSONReference.class, getterMethod, setterMethod);

            if (refAnno != null)
            {
                propertyInfo.setLinkIdProperty(refAnno.idProperty());
            }

            JSONTypeHint typeHintAnno = MethodUtil.getAnnotation(JSONTypeHint.class, getterMethod,
                setterMethod);
            Class<?>[] parameterTypes;
            Class paramType;
            if (typeHintAnno != null)
            {
                propertyInfo.setTypeHint(typeHintAnno.value());
            }
            else
            {
                if (adderMethod != null)
                {
                    propertyInfo.setTypeHint(adderMethod.getParameterTypes()[0]);
                }
            }

            propertyInfos.put(jsonPropertyName, propertyInfo);
        }


        return new JSONClassInfo(cls, propertyInfos, ctor, ctorTypeHint, postConstructMethod);
    }

    private boolean isBestMatchSetter(JavaObjectPropertyInfo pair, Method m)
    {
        if (pair.getGetterMethod() != null)
        {
            if (pair.getGetterMethod().getReturnType().equals(m.getParameterTypes()[0]))
            {
                return true;
            }
        }
        else
        {
            try
            {
                final Method getter = m.getDeclaringClass().getMethod(toGetterName(pair.getJavaPropertyName()));
                if (getter.getReturnType().equals(m.getParameterTypes()[0]))
                {
                    return true;
                }
            } catch (NoSuchMethodException e) {
                return true;
            }
        }

        return pair.getSetterMethod() == null;
    }

    private String toGetterName(String javaPropertyName) {
        StringBuilder methodName = new StringBuilder();
        methodName.append(GETTER_PREFIX)
                .append(javaPropertyName);
        final int firstPropNameLetter = GETTER_PREFIX.length();
        methodName.setCharAt(firstPropNameLetter, Character.toUpperCase(methodName.charAt(firstPropNameLetter)));
        return methodName.toString();
    }

    /**
     * Returns <code>true</code> if class a is a subclass of class b or if b is <code>null</code>.
     *
     * @param a
     * @param b
     * @return
     */
    static boolean isOverriding(Method a, Method b) {
        if (!a.getParameterTypes().equals(b.getParameterTypes())) {
            return false;
        }


        if (b.getDeclaringClass() == null)
        {
            return true;
        }

        Class<?> cls = a.getDeclaringClass();
        Class<?> superClass;
        while ((superClass = cls.getSuperclass()) != null)
        {
            cls = superClass;
            if (cls.equals(b))
            {
                return true;
            }
        }

        return false;
    }

}
