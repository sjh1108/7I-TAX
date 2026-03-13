pipeline {
    agent any

    stages {
        stage('Docker Build') {
            steps {
                dir('BE') {
                    sh 'docker build -t tax-backend .'
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([string(credentialsId: 'db-password', variable: 'DB_PASS')]) {
                    sh '''
                        docker stop tax-backend || true
                        docker rm tax-backend || true
                        docker run -d --name tax-backend \
                            --network ubuntu_default \
                            -p 18080:8080 \
                            -e DB_HOST=postgres \
                            -e DB_PORT=5432 \
                            -e DB_NAME=tax_db \
                            -e DB_USERNAME=postgres \
                            -e DB_PASSWORD=$DB_PASS \
                            -e REDIS_HOST=redis \
                            -e REDIS_PORT=6379 \
                            tax-backend
                    '''
                }
            }
        }
    }
}