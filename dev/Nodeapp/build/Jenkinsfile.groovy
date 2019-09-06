node {
  try {
	timestamps {
			notifyBuild('STARTED')
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
							credentialsId: 'github', 
							url: 'https://github.com/rohit6261/NodeApp.git',
							changelog: true
						]
					]
				])
			}	
		stage('Checkout-Devopscode') { // for display purposes
				checkout(
				[
					$class: 'GitSCM', 
					branches: [
						[name: '*/master']
					], 
					doGenerateSubmoduleConfigurations: false, 
					extensions: [
						[$class: 'CloneOption', depth: 0, noTags: false, reference: '', shallow: false, timeout: 40], 
						[$class: 'CheckoutOption', timeout: 40], 
					       [$class: 'RelativeTargetDirectory', relativeTargetDir: 'devops']
					], 
					gitTool: 'Default', 
					submoduleCfg: [], 
					userRemoteConfigs: [
						[
							credentialsId: 'github', 
							url: 'https://github.com/rohit6261/devops.git'
						]
					]
				])
			}	
			stage('npm install') {
				sh "npm install"
			}

		stage ('Collect Build Aritifacts') {
		def path = env.JOB_NAME.split('/')
		def pathlength = path.length
		def jobName = path[pathlength-1]
		JOB = jobName.toString()
			}
			
			stage('Get docker files') {
				sh "cp devops/dev/Nodeapp/docker/* ."
			}
			stage('Create-Docker-Images') {
			    NEW_BRANCH_NAME = sh(returnStdout: true, script: 'echo ${BRANCH_NAME} | sed -e \'s/\\//-/g\'').trim()
				echo "Starting to create docker images of all tools and pushing to Google cloud Repo"
				SHA = sh(returnStdout: true, script: "git log -n 1 --format=format:%H").trim()
				GITREPO = sh(script: 'echo $(git remote -v | awk \'{print $2}\' | tail -1)',returnStdout: true,)
				
				def customImage = docker.build("us.gcr.io/strong-land-247018/docker-images-${JOB}:${NEW_BRANCH_NAME}-${BUILD_NUMBER}", "--build-arg 'build_number=${NEW_BRANCH_NAME}-${BUILD_NUMBER}' --label 'branch=${NEW_BRANCH_NAME}' --label 'sha=${SHA}' --label 'gitrepo=${GITREPO}' --no-cache=true --force-rm --file Dockerfile .")
				
				customImage.push()
				
				sh "docker rmi us.gcr.io/strong-land-247018/docker-images-${JOB}:${NEW_BRANCH_NAME}-${BUILD_NUMBER}"
				echo "Finished creation of docker images of all tools and pushed to Google cloud Repo"
			}
	}
}
catch (e) {
    // If there was an exception thrown, the build failed
    currentBuild.result = "FAILED"
    throw e
  } finally {
    // Success or failure, always send notifications
    notifyBuild(currentBuild.result)
  }
}

def notifyBuild(String buildStatus = 'STARTED') {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def summary = "${subject} (${env.BUILD_URL})"
  
  body = """
    <html>
    <body bgcolor=' #aed6f1'>
    <font face='verdana' color='black'><p><pre>
    Hi Team,

    <p>Job '${env.JOB_NAME}' has a '${currentBuild.currentResult}' in BuildNumber :- '${env.BUILD_NUMBER}'</p>
	<p>Full logs are present at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>
	  
    <font face='verdana' color='red'>Note - this is an auto generated email, please don't reply to it.</font>

    <font face='verdana' color='blue'>Thanks,
    SimpleCI Team</font></pre></p></body></html>"""

  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
  } else {
    color = 'RED'
    colorCode = '#FF0000'
	//emailext attachLog: true, body: "${body}", mimeType: "text/html", compressLog: true, recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider'], [$class: 'FailingTestSuspectsRecipientProvider'], [$class: 'FirstFailingBuildSuspectsRecipientProvider'], [$class: 'DevelopersRecipientProvider']], subject: "${subject}", to: "rohitsnjob@gmail.com"
  }

  // Send notifications
 // slackSend (color: colorCode, message: summary)
}
