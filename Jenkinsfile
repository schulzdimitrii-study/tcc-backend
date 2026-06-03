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
            steps {
                sh 'docker rm -f redis-tests || true'
                sh 'docker run -d --name redis-tests -p 6380:6379 redis:7-alpine'
                script {
                    try {
                        echo "Cleaning target directory using Docker to avoid permission issues..."
                        sh 'docker run --rm -v "$(pwd)":/app -w /app alpine rm -rf target'
                        
                        echo "Running unit and verification tests with JaCoCo..."
                        sh 'REDIS_PORT=6380 ./mvnw clean verify'
                        
                        echo "Archiving JaCoCo report..."
                        archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                    } finally {
                        sh 'docker stop redis-tests && docker rm redis-tests'
                    }
                }
            }
        }

        stage('Deploy') {
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
                            docker-compose up -d --build api postgres redis
                        "
                    '''
                }
            }
        }
    }

    post {
        always {
            script {
                def mailTo = "${env.EMAIL_ADDRESSES_LIST}"
                def subject = ""
                def body = ""

                def commitHash = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                def commitAuthor = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                def commitMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                def buildDuration = currentBuild.durationString.replace(' and no more', '')
                
                def isDeploy = (env.BRANCH_NAME == 'main') ? "🚀 SIM — Implantado no Droplet (DigitalOcean)" : "NÃO — Deploy apenas na branch main"
                
                if (currentBuild.currentResult == 'SUCCESS') {
                    subject = "✅ Pipeline Sucesso: ${env.JOB_NAME} [${env.BRANCH_NAME}]"
                    body = """
                        CI/CD Pipeline executado com sucesso!
                        --------------------------------------------------
                        Projeto:          ${env.PIPELINE_NAME}
                        Job:              ${env.JOB_NAME}
                        Branch:           ${env.BRANCH_NAME}
                        Build:            #${env.BUILD_NUMBER}
                        Duração:          ${buildDuration}
                        
                        Detalhes do Commit:
                        - Hash:           ${commitHash}
                        - Autor:          ${commitAuthor}
                        - Mensagem:       ${commitMsg}
                        
                        Deploy:
                        - Status:         ${isDeploy}
                        
                        LINKS ÚTEIS
                        --------------------------------------------------
                        📋 Ver build no Jenkins   : ${env.BUILD_URL}
                        📊 Relatório de Cobertura  : ${env.BUILD_URL}artifact/target/site/jacoco/index.html
                        --------------------------------------------------
                        Mensagem automática gerada pelo pipeline de CI/CD
                        Projeto de TCC - INATEL – Gamificação e biofeedback em atividades físicas
                    """.stripIndent()
                } else {
                    subject = "❌ Pipeline Falhou: ${env.JOB_NAME} [${env.BRANCH_NAME}]"
                    body = """
                        CI/CD Pipeline falhou durante a execução!
                        --------------------------------------------------
                        Projeto:          ${env.PIPELINE_NAME}
                        Job:              ${env.JOB_NAME}
                        Branch:           ${env.BRANCH_NAME}
                        Build:            #${env.BUILD_NUMBER}
                        Duração:          ${buildDuration}
                        
                        Detalhes do Commit:
                        - Hash:           ${commitHash}
                        - Autor:          ${commitAuthor}
                        - Mensagem:       ${commitMsg}
                        
                        Deploy:
                        - Status:         ${isDeploy}
                        
                        LINKS ÚTEIS
                        --------------------------------------------------
                        📋 Ver build no Jenkins:   ${env.BUILD_URL}
                        📊 Relatório de Cobertura  : ${env.BUILD_URL}artifact/target/site/jacoco/index.html
                        --------------------------------------------------
                        Mensagem automática gerada pelo pipeline de CI/CD
                        Projeto de TCC - INATEL - Gamificação e biofeedback em atividades físicas
                    """.stripIndent()
                }
                
                mail to: mailTo,
                     subject: subject,
                     body: body
            }
        }
    }
}
