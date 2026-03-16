pipeline {
    agent any

    stages {
        stage('BE - Docker Build') {
            steps {
                dir('BE') {
                    sh 'DOCKER_BUILDKIT=1 docker build -t tax-backend .'
                }
            }
        }

        stage('FE - Docker Build') {
            steps {
                dir('FE') {
                    sh 'DOCKER_BUILDKIT=1 docker build -t tax-frontend .'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    docker stop tax-backend || true
                    docker rm tax-backend || true
                    docker run -d --name tax-backend \
                        --network ubuntu_default \
                        -p 18080:8080 \
                        --env-file /home/ubuntu/.env.backend \
                        tax-backend

                    docker stop tax-frontend || true
                    docker rm tax-frontend || true
                    docker run -d --name tax-frontend \
                        --network ubuntu_default \
                        -p 3000:80 \
                        tax-frontend
                '''
            }
        }
    }

    post {
        success {
            mattermostSend(
                color: '#2EB67D',
                message: "✅ 배포 성공: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
            )
        }
        failure {
            mattermostSend(
                color: '#E01E5A',
                message: "❌ 빌드 실패: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
            )
        }
    }
}