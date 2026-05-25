// =============================================================================
// Jenkinsfile — gitops-app
//
// CREDENTIALS (configure in Jenkins → Manage Jenkins → Credentials):
//   dockerhub-credentials  : Username/Password — Docker Hub login
//   github-token           : Secret Text — GitHub PAT for pushing to gitops-config
//   git-user-email         : Secret Text — email for git commit in config repo
//   git-user-name          : Secret Text — name for git commit in config repo
//   github-app-repo        : Username/Password (pat) — gitHub login
// =============================================================================

pipeline {

    agent any

    environment {
        IMAGE_NAME = "tejaspise/gitops-app"
        IMAGE_TAG  = "${GIT_COMMIT}"
        CONFIG_REPO_URL  = "https://github.com/Tejaspise93/gitops-config.git"
        CONFIG_REPO_NAME = "gitops-config"
        VALUES_FILE_PATH = "environments/dev/values.yaml"
    }

    options {
        // Discard old builds — keeps Jenkins storage from growing unbounded.
        // Keeps last 10 build logs and last 5 artifacts.
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))

        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()
    }

    stages {

        // =====================================================================
        // STAGE 1 — CHECKOUT
        // =====================================================================
        stage('Checkout') {
            steps {
                checkout scm

                sh '''
                    echo "========================================="
                    echo "Branch  : ${GIT_BRANCH}"
                    echo "Commit  : ${GIT_COMMIT}"
                    echo "Build # : ${BUILD_NUMBER}"
                    echo "========================================="
                '''
            }
        }

        // =====================================================================
        // STAGE 2 — MAVEN BUILD
        // =====================================================================
        stage('Maven Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
            post {
                success {
                    // Archive the fat JAR as a Jenkins build artifact.
                    // This allows download of the exact JAR from any build
                    // directly from the Jenkins UI.
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        // =====================================================================
        // STAGE 3 — UNIT TESTS
        // =====================================================================
        stage('Unit Tests') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    // Publish JUnit XML test results to Jenkins regardless of pass/fail.
                    junit '**/target/surefire-reports/TEST-*.xml'
                }
                failure {
                    echo 'Unit tests failed. Review the test report in Jenkins before proceeding.'
                }
            }
        }

        // =====================================================================
        // STAGE 4 — SONARQUBE ANALYSIS (PLACEHOLDER)
        // =====================================================================
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn -B sonar:sonar \
                            -Dsonar.projectKey=gitops-app \
                            -Dsonar.projectName="GitOps App" \
                            -Dsonar.java.coveragePlugin=jacoco \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml\
                    '''
                }
                waitForQualityGate abortPipeline: true
            }
        }

        // =====================================================================
        // STAGE 5 — DOCKER BUILD
        // =====================================================================
        stage('Docker Build') {
            steps {
                sh """
                    echo "Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"

                    docker build \
                        --label "build.number=${BUILD_NUMBER}" \
                        --label "git.commit=${GIT_COMMIT}" \
                        --label "git.branch=${GIT_BRANCH}" \
                        -t ${IMAGE_NAME}:${IMAGE_TAG} \
                        -t ${IMAGE_NAME}:latest \
                        .

                    echo "Docker image built successfully."
                    docker images ${IMAGE_NAME}
                """
            }
        }

        // =====================================================================
        // STAGE 6 — DOCKER PUSH
        // =====================================================================
        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin

                        echo "Pushing ${IMAGE_NAME}:${IMAGE_TAG} to Docker Hub..."
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}

                        echo "Pushing ${IMAGE_NAME}:latest to Docker Hub..."
                        docker push ${IMAGE_NAME}:latest

                        echo "Push complete. Logging out of Docker Hub."
                        docker logout
                    """
                }
            }
            post {
                always {
                    // Remove local images after push regardless of success or failure.
                    sh """
                        docker rmi ${IMAGE_NAME}:${IMAGE_TAG} || true
                        docker rmi ${IMAGE_NAME}:latest       || true
                    """
                }
            }
        }

        // =====================================================================
        // STAGE 7 — UPDATE IMAGE TAG IN GITOPS CONFIG REPO
        // =====================================================================
        stage('Update Image Tag in Config Repo') {
            steps {
                withCredentials([
                    string(credentialsId: 'github-token',     variable: 'GH_TOKEN'),
                    string(credentialsId: 'git-user-email',   variable: 'GIT_EMAIL'),
                    string(credentialsId: 'git-user-name',    variable: 'GIT_NAME')
                ]) {
                    sh """
                        # Clean up any previous clone of the config repo
                        rm -rf ${CONFIG_REPO_NAME}

                        git clone https://${GH_TOKEN}@github.com/Tejaspise93/${CONFIG_REPO_NAME}.git

                        cd ${CONFIG_REPO_NAME}

                        git config user.email "${GIT_EMAIL}"
                        git config user.name  "${GIT_NAME}"

                        yq e '.image.tag = "${IMAGE_TAG}"' -i ${VALUES_FILE_PATH}

                        echo "Updated ${VALUES_FILE_PATH}:"
                        grep 'tag:' ${VALUES_FILE_PATH}
                        cat ${VALUES_FILE_PATH}

                        git add ${VALUES_FILE_PATH}
                        git commit -m "ci: update gitops-app image tag to ${IMAGE_TAG} [build #${BUILD_NUMBER}]"
                        git push origin main

                        echo "Config repo updated. ArgoCD will detect and sync shortly."

                        # Clean up the cloned repo from the workspace
                        cd ..
                        rm -rf ${CONFIG_REPO_NAME}
                    """
                }
            }
        }

    }

    post {

        success {
            echo """
            =========================================
            PIPELINE SUCCEEDED
            =========================================
            Application : ${IMAGE_NAME}
            Image Tag   : ${IMAGE_TAG}
            Branch      : ${GIT_BRANCH}
            Build #     : ${BUILD_NUMBER}
            =========================================
            """
        }

        failure {
            echo """
            =========================================
            PIPELINE FAILED
            =========================================
            Application : ${IMAGE_NAME}
            Branch      : ${GIT_BRANCH}
            Build #     : ${BUILD_NUMBER}
            Commit      : ${GIT_COMMIT}
            =========================================
            """
        }

        always {
            cleanWs(
                cleanWhenSuccess: true,
                cleanWhenFailure: true,
                cleanWhenAborted: true,
                deleteDirs:       true,
                notFailBuild:     true
            )
        }
    }

}