pipeline {
    agent any

    stages {
        stage('BE - Docker Build') {
            steps {
                dir('BE') {
                    sh 'docker build -t tax-backend .'
                }
            }
        }

        stage('FE - Docker Build') {
            steps {
                dir('FE') {
                    sh 'docker build -t tax-frontend .'
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
}