package com.cumulocity.microservice.security.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class ErrorController extends BasicErrorController {

    public ErrorController(ErrorAttributes errorAttributes, ServerProperties serverProperties, ObjectProvider<ErrorViewResolver> errorViewResolvers) {
        super(errorAttributes, serverProperties.getError(), errorViewResolvers.orderedStream().collect(Collectors.toList()));
    }

    @Override
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = getStatus(request);
        Map<String, Object> model = Collections
                .unmodifiableMap(getErrorAttributes(request, isIncludeStacktraceErrorAttribute(request)));
        response.setStatus(status.value());
        ModelAndView modelAndView = resolveErrorView(request, response, status, model);
        return (modelAndView != null) ? modelAndView : new ModelAndView(new HtmlErrorView(), model);
    }

    private ErrorAttributeOptions isIncludeStacktraceErrorAttribute(HttpServletRequest request) {
        if (isIncludeStackTrace(request, MediaType.TEXT_HTML)) {
            return ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE);
        }
        return ErrorAttributeOptions.defaults();
    }

    private static class HtmlErrorView implements View {

        private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

        @Override
        public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
                throws Exception {
            if (response.isCommitted()) {
                return;
            }
            response.setContentType(TEXT_HTML_UTF8.toString());
            StringBuilder builder = new StringBuilder();
            if (response.getContentType() == null) {
                response.setContentType(getContentType());
            }
            builder.append("<html><body>")
                    .append("<h1>HTTP ERROR ").append(htmlEscape(model.get("error"))).append(" ").append(htmlEscape(model.get("status"))).append("</h1>")
                    .append("<div><b>Time:</b> ").append(model.get("timestamp")).append("</div>");
            if (model.get("message") != null) {
                builder.append("<div><b>Message:</b> ").append(htmlEscape(model.get("message"))).append("</div>");
            }
            if (model.get("trace") != null) {
                builder.append("<div style='white-space:pre-wrap;'>").append(htmlEscape(model.get("trace"))).append("</div>");
            }
            builder.append("</body></html>");
            response.getWriter().append(builder.toString());
        }

        private String htmlEscape(Object input) {
            return (input != null) ? HtmlUtils.htmlEscape(input.toString()) : null;
        }

        @Override
        public String getContentType() {
            return "text/html";
        }

    }
}
