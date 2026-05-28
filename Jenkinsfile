pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        disableConcurrentBuilds()
    }

    triggers {
        githubPush()
    }

    environment {
        PIPELINE_NAME = 'TCC Backend — CI/CD Pipeline'
        DO_SSH_CREDS  = credentials('do-ssh-credentials')
        MAIL_CREDS    = credentials('mail-credentials')
        
        DO_HOST       = credentials('do-host')
        DO_USERNAME   = credentials('do-username')
        PROJECT_PATH  = credentials('project-path')
        EMAIL_ADDRESSES_LIST = credentials('emails-list')
    }

    stages {
        stage('Build') {
            steps {
                echo "Building project..."
                sh 'chmod +x mvnw && ./mvnw clean install -DskipTests'
            }
        }

        stage('Tests & Coverage') {
            parallel {
                stage('Run Tests') {
                    steps {
                        sh 'docker rm -f redis-tests || true'
                        sh 'docker run -d --name redis-tests -p 6380:6379 redis:7-alpine'
                        script {
                            try {
                                echo "Running unit tests..."
                                sh 'REDIS_PORT=6380 ./mvnw clean test'
                            } finally {
                                sh 'docker stop redis-tests && docker rm redis-tests'
                            }
                        }
                    }
                }
                stage('Code Coverage') {
                    steps {
                        sh 'docker rm -f redis-coverage || true'
                        sh 'docker run -d --name redis-coverage -p 6381:6379 redis:7-alpine'
                        script {
                            try {
                                echo "Running coverage tests with JaCoCo..."
                                sh 'REDIS_PORT=6381 ./mvnw clean verify'
                                echo "Archiving JaCoCo report..."
                                archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                            } finally {
                                sh 'docker stop redis-coverage && docker rm redis-coverage'
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                echo "Building production JAR..."
                sh './mvnw clean package -DskipTests'
                
                sshagent(credentials: ['do-ssh-credentials']) {
                    echo "Copying JAR to DigitalOcean..."
                    sh 'scp -o StrictHostKeyChecking=no target/*.jar ${DO_USERNAME}@${DO_HOST}:${PROJECT_PATH}/target/'
                    
                    echo "Restarting Docker Containers on Droplet..."
                    sh '''
                        ssh -o StrictHostKeyChecking=no ${DO_USERNAME}@${DO_HOST} "
                            cd ${PROJECT_PATH} &&
                            git pull origin main &&
                            docker-compose down &&
                            docker-compose up -d --build
                        "
                    '''
                }
            }
        }
    }

    post {
        always {
            script {
                def mailTo = ${EMAIL_ADDRESSES_LIST}
                def subject = ""
                def body = ""
                
                if (currentBuild.currentResult == 'SUCCESS') {
                    subject = "✅ CI Passed — ${env.JOB_NAME} (${env.BRANCH_NAME})"
                    body = """
                        ✅ CI pipeline completed successfully!

                        Repository : ${env.JOB_NAME}
                        Branch     : ${env.BRANCH_NAME}
                        Build      : #${env.BUILD_NUMBER}
                        Result     : ${currentBuild.currentResult}

                        View run: ${env.BUILD_URL}
                    """.stripIndent()
                } else {
                    subject = "❌ CI Failed — ${env.JOB_NAME} (${env.BRANCH_NAME})"
                    body = """
                        ❌ CI pipeline failed!

                        Repository : ${env.JOB_NAME}
                        Branch     : ${env.BRANCH_NAME}
                        Build      : #${env.BUILD_NUMBER}
                        Result     : ${currentBuild.currentResult}

                        View run: ${env.BUILD_URL}
                    """.stripIndent()
                }
                
                mail to: mailTo,
                     subject: subject,
                     body: body
            }
        }
    }
}
