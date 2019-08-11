node {
    try {
		BRANCH_NAME = "${env.BRANCH_NAME}"
        if("${BRANCH_NAME}" =~ "master") {
   			stage('Checkout-Code') { // for display purposes
				checkout(
	[
					$class: 'GitSCM', 
					branches: [
						[name: '*/${BRANCH_NAME}']
					], 
					doGenerateSubmoduleConfigurations: false, 
					extensions: [
						[$class: 'CloneOption', depth: 0, noTags: false, reference: '', shallow: false, timeout: 40], 
						[$class: 'CheckoutOption', timeout: 40]
					], 
					gitTool: 'Default', 
					submoduleCfg: [], 
					userRemoteConfigs: [
						[
							credentialsId: 'rohit', 
							url: 'https://github.com/rohit6261/test12.git',
							changelog: true
						]
					]
				])
			}

    stage ('Fetch version details of nexus & package.json') {
		def path = env.JOB_NAME.split('/')
		def pathlength = path.length
		def jobName = path[pathlength-1]
		JOB = jobName.toString()
		env.JOB = JOB
		sh "echo $JOB"
    sh "curl -o version-checker.json -X GET -H 'Authorization:Basic YWRtaW46U2FwaWVudCMxMjM=' https://nexus.in.pscloudhub.com/service/rest/v1/search?repository=npm-ps"
	NEXUS_VERSION = sh(script: 'echo $(cat version-checker.json | jq --arg JOB $JOB -r ".items[] | select(.name | contains (\\"$JOB\\"))? | .version")', returnStdout: true,).trim()
	sh "echo 'The version uploaded on nexus is ${NEXUS_VERSION }'"
	PACKAGE_VERSION = sh(script: 'echo $(cat package.json  | grep version  | head -1  | awk -F: \'{ print $2 }\'  | sed \'s/[",]//g\')', returnStdout: true,).trim()
	sh "echo 'The version needs to be uploaded is ${PACKAGE_VERSION }'"
			}
    stage ('Compare version') {
    	def availVersion = "$PACKAGE_VERSION"
        def nexusVersion   = "$NEXUS_VERSION"
        def availTokens = availVersion.split('\\.')
        def nexusTokens   = nexusVersion.split('\\.')
        def availSize   = availTokens.size()
        def nexusSize     = nexusTokens.size()
    def maxSize     = Math.max(availSize, nexusSize)
    for (int i = 1; i <= maxSize; i++) {
    def availItem = ((i <= availSize) ? availTokens[i - 1] : 0)
    def nexusItem    = ((i <= nexusSize)  ? nexusTokens[i - 1]   : 0)
    print "Avail: ${availItem} -> nexus: ${nexusItem}\n"
    if ((nexusItem > availItem) || ( (i == maxSize) && (nexusItem >= availItem) )) {
        print "Npm publish is not needed.\n"
        return
       }
   }
   print "Npm publish is needed!\n"
	    sh "npm publish"
    }
}
else {
	        echo "Not a master branch"
		}

}catch (e) {
        // If there was an exception thrnexus, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {
    }
}
