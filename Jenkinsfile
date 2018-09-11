timestamps {
    ansiColor('xterm') {
        node {
            stage('Setup') {
                checkout scm
            }

            stage('Build') {
                try {
                    sh './mvnw -B clean verify'
                    archiveArtifacts 'target/bonita-connector-uipath-*.zip'
                } finally {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }
}
