// =============================================================================
// Jenkinsfile — Declarative Pipeline for gitops-app
//
// PIPELINE OVERVIEW:
//   1. Checkout         → Pull source code from SCM
//   2. Maven Build      → Compile the application
//   3. Unit Tests       → Run test suite, publish results
//   4. SonarQube        → Static analysis (PLACEHOLDER — disabled for now)
//   5. Docker Build     → Build image tagged with Git commit SHA
//   6. Docker Push      → Push image to Docker Hub
//   7. Update Config    → Patch image tag in gitops-config repo (GitOps loop)
//
// GITOPS LOOP EXPLAINED:
//   This pipeline does NOT deploy directly. It updates a SEPARATE Git repo
//   (gitops-config) which holds Helm values / K8s manifests. ArgoCD (Phase 2)
//   watches that repo and syncs the cluster to match it.
//   This separation of "app repo" and "config repo" is the core GitOps pattern.
//   It means Git is the single source of truth for what is running in the cluster.
//
// CREDENTIALS (configure in Jenkins → Manage Jenkins → Credentials):
//   dockerhub-credentials  : Username/Password — Docker Hub login
//   github-token           : Secret Text — GitHub PAT for pushing to gitops-config
//   git-user-email         : Secret Text — email for git commit in config repo
//   git-user-name          : Secret Text — name for git commit in config repo
// =============================================================================

pipeline {

    // 'agent any' — run on any available Jenkins agent/node.
    // For production, replace with a labelled agent or a Docker agent
    // to ensure a consistent build environment:
    //   agent { label 'docker-enabled' }
    agent any

    // =========================================================================
    // ENVIRONMENT VARIABLES
    // Centralised here so values are easy to find, change, and audit.
    // Never hardcode credentials here — use credentials() binding instead.
    // =========================================================================
    environment {

        // Docker Hub image name — replace YOUR_DOCKERHUB_USERNAME
        // Format: <dockerhub-username>/<repository-name>
        IMAGE_NAME = "YOUR_DOCKERHUB_USERNAME/gitops-app"

        // Tag the image with the Git commit SHA for full traceability.
        // GIT_COMMIT is a built-in Jenkins environment variable (40-char SHA).
        // Using SHA instead of 'latest' means:
        //   - Every image is uniquely and immutably identified
        //   - You can roll back to any previous commit by its SHA
        //   - 'latest' is an anti-pattern in GitOps — it's mutable and
        //     makes it impossible to know exactly what is deployed
        IMAGE_TAG  = "${GIT_COMMIT}"

        // Separate config repo details — this is the GitOps config repository
        // that ArgoCD (Phase 2) will watch. Replace with your actual values.
        CONFIG_REPO_URL  = "https://github.com/YOUR_USERNAME/gitops-config.git"
        CONFIG_REPO_NAME = "gitops-config"

        // Path inside the config repo where the image tag lives.
        // ArgoCD + Helm will read this file to know which image to deploy.
        VALUES_FILE_PATH = "environments/dev/values.yaml"
    }

    // =========================================================================
    // BUILD OPTIONS
    // =========================================================================
    options {
        // Discard old builds — keeps Jenkins storage from growing unbounded.
        // Keeps last 10 build logs and last 5 artifacts.
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))

        // Abort the build if it runs longer than 30 minutes.
        // Prevents hung builds from blocking the executor indefinitely.
        timeout(time: 30, unit: 'MINUTES')

        // Prevents concurrent builds of the same branch.
        // Important: two parallel builds could push conflicting image tags
        // or create a race condition in the config repo git push.
        disableConcurrentBuilds()

        // Add timestamps to all console output — essential for diagnosing
        // slow stages and correlating with external system logs.
        timestamps()
    }

    stages {

        // =====================================================================
        // STAGE 1 — CHECKOUT
        // =====================================================================
        stage('Checkout') {
            steps {
                // checkout scm uses the SCM configuration from the Jenkins job
                // (the repo URL and credentials configured in the job definition).
                // In a Multibranch Pipeline, this automatically checks out the
                // correct branch that triggered the build.
                checkout scm

                // Print the commit SHA and branch for build traceability.
                // These appear in the Jenkins console log and help correlate
                // a build with a specific commit in GitHub.
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
                // '-B' = batch mode: suppresses Maven's download progress bars.
                //        Essential for clean, readable CI logs.
                // '-DskipTests' here because tests run in their own dedicated
                //        stage below, giving us separate pass/fail reporting.
                sh 'mvn -B clean package -DskipTests'
            }
            post {
                success {
                    // Archive the fat JAR as a Jenkins build artifact.
                    // This allows you to download the exact JAR from any build
                    // directly from the Jenkins UI — useful for debugging.
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        // =====================================================================
        // STAGE 3 — UNIT TESTS
        // =====================================================================
        stage('Unit Tests') {
            steps {
                // Run only the test phase — compilation already done above.
                sh 'mvn -B test'
            }
            post {
                always {
                    // Publish JUnit XML test results to Jenkins regardless of
                    // pass/fail. Jenkins parses these and renders a test trend
                    // graph over time in the job dashboard.
                    // Maven Surefire plugin writes results to target/surefire-reports/.
                    junit '**/target/surefire-reports/TEST-*.xml'
                }
                failure {
                    echo 'Unit tests failed. Review the test report in Jenkins before proceeding.'
                }
            }
        }

        // =====================================================================
        // STAGE 4 — SONARQUBE ANALYSIS (PLACEHOLDER)
        //
        // This stage is intentionally disabled for Phase 1.
        // SonarQube provides static code analysis: code smells, bugs,
        // security vulnerabilities (OWASP), and test coverage reporting.
        //
        // TO ENABLE IN PHASE 2:
        //   1. Deploy SonarQube (Docker Compose or K8s)
        //   2. Add 'sonar-token' Secret Text credential in Jenkins
        //   3. Install the SonarQube Scanner plugin in Jenkins
        //   4. Configure SonarQube server under:
        //        Jenkins → Manage Jenkins → Configure System → SonarQube servers
        //   5. Uncomment the withSonarQubeEnv block below
        //   6. Optionally add a Quality Gate check:
        //        waitForQualityGate abortPipeline: true
        //      This will fail the build if code quality drops below threshold.
        // =====================================================================
        stage('SonarQube Analysis') {
            steps {
                echo 'SonarQube analysis is disabled for Phase 1.'
                echo 'Placeholder stage — will be enabled in Phase 2 (Observability).'

                // --- UNCOMMENT BELOW TO ENABLE ---
                // withSonarQubeEnv('SonarQube') {
                //     sh '''
                //         mvn -B sonar:sonar \
                //             -Dsonar.projectKey=gitops-app \
                //             -Dsonar.projectName="GitOps App" \
                //             -Dsonar.java.coveragePlugin=jacoco \
                //             -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                //     '''
                // }
                // waitForQualityGate abortPipeline: true
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
                // NOTE on tagging with 'latest':
                // We tag with both SHA and 'latest' here for local convenience.
                // In the GitOps config repo, we always reference the SHA tag —
                // 'latest' is never used in values.yaml. This ensures ArgoCD
                // deploys a specific, auditable image, not a floating tag.
            }
        }

        // =====================================================================
        // STAGE 6 — DOCKER PUSH
        // =====================================================================
        stage('Docker Push') {
            steps {
                // withCredentials binds the Jenkins credential to environment
                // variables scoped ONLY to this block. The credential value is:
                //   - Never printed in console logs (Jenkins masks it)
                //   - Not available outside this block
                //   - Never stored in the Jenkinsfile itself
                //
                // usernamePassword unpacks a Username/Password credential into
                // two separate env vars: DOCKER_USER and DOCKER_PASS.
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        # Login to Docker Hub.
                        # '--password-stdin' avoids the password appearing in
                        # the process list (ps aux) — more secure than -p flag.
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
        }

        // =====================================================================
        // STAGE 7 — UPDATE IMAGE TAG IN GITOPS CONFIG REPO
        //
        // THIS IS THE CORE GITOPS STEP.
        //
        // What happens here:
        //   1. Clone the separate gitops-config repository
        //   2. Use 'sed' to replace the old image tag with the new SHA tag
        //      in environments/dev/values.yaml
        //   3. Commit the change with a meaningful message
        //   4. Push back to the config repo
        //
        // What happens next (Phase 2 — ArgoCD):
        //   ArgoCD watches the gitops-config repo. When it detects this commit,
        //   it automatically syncs the Kubernetes cluster to the new image tag.
        //   The pipeline NEVER kubectl-applies anything directly.
        //   Git is the source of truth. ArgoCD is the deploy agent.
        //
        // This pattern is called the "push model" for GitOps:
        //   CI pushes to config repo → ArgoCD pulls and reconciles.
        // =====================================================================
        stage('Update Image Tag in Config Repo') {
            steps {
                withCredentials([
                    // GitHub PAT (Personal Access Token) for pushing to the
                    // config repo. Store as a 'Secret Text' credential in Jenkins.
                    // The PAT needs 'repo' scope on the gitops-config repository.
                    string(credentialsId: 'github-token',     variable: 'GH_TOKEN'),
                    string(credentialsId: 'git-user-email',   variable: 'GIT_EMAIL'),
                    string(credentialsId: 'git-user-name',    variable: 'GIT_NAME')
                ]) {
                    sh """
                        # Clean up any previous clone of the config repo
                        rm -rf ${CONFIG_REPO_NAME}

                        # Clone using the GitHub token embedded in the HTTPS URL.
                        # Token-in-URL is standard for CI automation with GitHub.
                        # The token is masked in Jenkins logs by the credentials binding.
                        git clone https://${GH_TOKEN}@github.com/YOUR_USERNAME/${CONFIG_REPO_NAME}.git

                        cd ${CONFIG_REPO_NAME}

                        # Configure git identity for the commit.
                        # These values appear in the git log of the config repo,
                        # making it clear which CI system made the change.
                        git config user.email "${GIT_EMAIL}"
                        git config user.name  "${GIT_NAME}"

                        # ---------------------------------------------------------
                        # SED — Replace the image tag in values.yaml
                        #
                        # Target line format in environments/dev/values.yaml:
                        #   tag: "abc1234..."   (any existing SHA or value)
                        #
                        # sed command breakdown:
                        #   -i          : edit the file in-place
                        #   's|...|...|': substitute pattern with replacement
                        #                 using '|' as delimiter (avoids conflict
                        #                 with '/' characters in image names)
                        #   tag: ".*"   : matches the tag line with any current value
                        #   tag: "${IMAGE_TAG}" : replaces with the new SHA tag
                        #
                        # Your values.yaml should contain a line like:
                        #   image:
                        #     repository: YOUR_DOCKERHUB_USERNAME/gitops-app
                        #     tag: "placeholder"
                        # ---------------------------------------------------------
                        sed -i 's|tag: ".*"|tag: "${IMAGE_TAG}"|g' ${VALUES_FILE_PATH}

                        echo "Updated ${VALUES_FILE_PATH}:"
                        grep 'tag:' ${VALUES_FILE_PATH}

                        # Stage, commit, and push the change.
                        # The commit message includes the build number and SHA
                        # for full traceability — you can find this commit in
                        # the config repo and know exactly which Jenkins build
                        # triggered it and what image it points to.
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
    // End of stages

    // =========================================================================
    // POST BLOCK
    // Runs after all stages complete, regardless of outcome.
    // 'always' > 'success'/'failure' — always block runs first.
    // =========================================================================
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
            -----------------------------------------
            Image pushed to Docker Hub.
            Config repo updated with new image tag.
            ArgoCD will sync the cluster shortly.
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
            -----------------------------------------
            Review the stage logs above to identify
            the failure point. Common causes:
              - Unit test failures (Stage 3)
              - Docker daemon not running on agent
              - Invalid Docker Hub credentials
              - GitHub token lacks repo push access
              - values.yaml tag line format mismatch
            =========================================
            """
        }

        always {
            // Clean the Jenkins workspace after every build —
            // success or failure. Prevents disk exhaustion on the
            // Jenkins agent over time, especially with large Docker
            // layer caches and Maven target directories.
            cleanWs()
        }
    }

}