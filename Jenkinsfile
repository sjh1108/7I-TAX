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
                sh '''
                    docker stop tax-backend || true
                    docker rm tax-backend || true
                    docker run -d --name tax-backend \
                        --network ubuntu_default \
                        -p 18080:8080 \
                        --env-file /home/ubuntu/.env.backend \
                        tax-backend
                '''
            }
        }
    }
}