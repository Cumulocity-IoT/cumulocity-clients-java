@Library('c8y-common-steps') _

def adminCredentialsId
def testInstanceDomain
def systemVersion
def testBranch

pipeline {
    agent {
        kubernetes {
            yaml '''
        apiVersion: v1
        kind: Pod
        spec:
          imagePullSecrets:
          - name: "ci-registry-pull"
          containers:
          - name: "java"
            image: "registry.stage.c8y.io/ci/jdk:17"
            command: ["/bin/sh"]
            args: ["-c", "cat"]
            tty: true
            env:
            - name: JENKINS_URL
              value: http://jenkins.ci.svc.cluster.local:8080/
            - name: MAVEN_USER_HOME
              value: /home/jenkins/agent
            - name: DOCKER_HOST
              value: tcp://localhost:2375
            workingDir: "/home/jenkins/agent"
            resources:
              requests:
                cpu: 2
                memory: 3Gi
              limits:
                cpu: 3
                memory: 3Gi
        '''
        }
    }
    options {
        timeout(time: 4, unit: 'HOURS')
        skipDefaultCheckout()
    }
    parameters {
        string(name: 'TEST_BRANCH', defaultValue: '', description: 'tests branch/revision to checkout; determined from env.ghprbSourceBranch when left empty')
        string(name: 'TEST_DOMAIN', defaultValue: '', description: 'FQDN of the test environment; determined from env.ghprbSourceBranch when left empty')
        choice(name: 'ADMIN_CREDENTIALS', choices: ['post-merge-admin', 'e2eAdmin'], description: 'Admin credentials ID; only used when TEST_DOMAIN is given')
    }
    stages {
        stage('Initialize') {
            steps{
                script {
                    if (!params.TEST_DOMAIN.isEmpty()) {
                        testInstanceDomain = params.TEST_DOMAIN
                        adminCredentialsId = params.ADMIN_CREDENTIALS
                    } else if (env.ghprbSourceBranch.contains('/Staging')) {
                        testInstanceDomain = stagingTestingDomain(env.ghprbSourceBranch)
                        adminCredentialsId = 'e2eAdmin'
                    } else {
                        testInstanceDomain = postMergeTestingDomain(env.ghprbSourceBranch)
                        adminCredentialsId = 'post-merge-admin'
                    }
                }
            }
        }

        stage('Get platform version') {
            steps {
                echo "Running from branch ${env.ghprbSourceBranch} on ${testInstanceDomain}"
                script {
                    if (!params.TEST_BRANCH.isEmpty()) {
                        testBranch = params.TEST_BRANCH
                    } else if (['2024', '10.18.0.x', '10.17.0.x', '10.16.0.x', '10.15.0.x']
                        .any {env.ghprbSourceBranch.startsWith(it)}) {
                        // for versions 2024 and before sdk is a part of cumulocity component
                        systemVersion = GetVersionFromSourceBranch('cumulocity')
                        testBranch = "refs/tags/clients-java-${systemVersion}"
                    } else {
                        // later it's separate component so take the version from its descriptor
                        systemVersion = GetVersionFromSourceBranch('java-sdk')
                        testBranch = "refs/tags/clients-java-${systemVersion}"
                    }
                    echo "Running tests for branch ${testBranch} on ${testInstanceDomain}"
                }

            }
        }

        stage('Checkout') {
            steps {
                echo 'Checkout current version of jenkins scripts'
                checkout(scm: [
                    $class: 'GitSCM',
                    branches: scm.branches,
                    userRemoteConfigs: scm.userRemoteConfigs,
                    extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: '.jenkins/*']]]]
                ])
                sh 'mv ${WORKSPACE}/.jenkins ${WORKSPACE_TMP}/'

                echo "Checkout version ${systemVersion} of tests"
                checkout(scm: [
                    $class: 'GitSCM',
                    branches: [[name: testBranch]],
                    userRemoteConfigs: scm.userRemoteConfigs,
                    extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: '/*'], [path: '!.jenkins']]]]
                ])
                sh 'mv ${WORKSPACE_TMP}/.jenkins ${WORKSPACE}/'
            }
        }

        stage('Run tests') {
            environment {
                MVN_SETTINGS = credentials('maven-settings')
                MAVEN_PROFILES = 'integration'
                ADMIN_CREDENTIALS = credentials("${adminCredentialsId}")
            }
            steps {
                container('java') {
                    script {
                        catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                            sh """\
                                .jenkins/scripts/mvn.sh verify \\
                                    --file . --projects java-client --also-make \\
                                    --define 'cumulocity.host=http://${testInstanceDomain}:8111'
                               """
                        }
                    }
                }
            }
            post {
                always {
                    junit(testResults: '**/TEST-*.xml', keepLongStdio: true, allowEmptyResults: true)
                }
                success {
                    createSummary(icon: 'green.gif', text: 'SDK tests passed')
                }
                failure {
                    createSummary(icon: 'error.gif', text: 'SDK tests failed')
                }
            }
        }
    }
    post {
        always {
            script {
                build job: "cucumber-tenants-cleanup",
                    parameters: [
                        string(name: "DOMAIN", value: "${testInstanceDomain}"),
                        string(name: "TENANT_PREFIX", value: "plama-sdk"),
                        string(name: "TEST_MODE", value: "false")
                    ],
                    propagate: false, wait: false

            }
        }
    }
}
