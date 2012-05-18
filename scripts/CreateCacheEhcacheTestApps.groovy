includeTargets << grailsScript('_GrailsBootstrap')

functionalTestPluginVersion = '1.2.7'
projectfiles = new File(basedir, 'webtest/projectFiles')
grailsHome = null
dotGrails = null
grailsVersion = null
projectDir = null
appName = null
pluginVersion = null
pluginZip = null
testprojectRoot = null
deleteAll = false

target(createCacheEhcacheTestApps: 'Creates test apps for functional tests') {

	def configFile = new File(basedir, 'testapps.config.groovy')
	if (!configFile.exists()) {
		error "$configFile.path not found"
	}

	new ConfigSlurper().parse(configFile.text).each { name, config ->
		printMessage "\nCreating app based on configuration $name: ${config.flatten()}\n"
		init name, config
		createApp()
		installPlugins()
		copyProjectFiles()
		copyTests()
	}
}

private void createApp() {

	ant.mkdir dir: projectDir

	deleteDir testprojectRoot
	deleteDir "$dotGrails/projects/$appName"

	callGrails(grailsHome, projectDir, 'dev', 'create-app') {
		ant.arg value: appName
	}
}

private void installPlugins() {

	File buildConfig = new File(testprojectRoot, 'grails-app/conf/BuildConfig.groovy')
	String contents = buildConfig.text
	contents = contents.replace('grails.project.class.dir = "target/classes"', "grails.project.work.dir = 'target'")
	contents = contents.replace('grails.project.test.class.dir = "target/test-classes"', '')
	contents = contents.replace('grails.project.test.reports.dir = "target/test-reports"', '')

	buildConfig.withWriter { it.writeLine contents }

	File configGroovy = new File(testprojectRoot, 'grails-app/conf/Config.groovy')
	contents = configGroovy.text
	contents += '''

grails.cache.config = {
	cache {
		name 'message'
		eternal false
		overflowToDisk true
		maxElementsInMemory 10000
		maxElementsOnDisk 10000000
	}
}

'''

	configGroovy.withWriter { it.writeLine contents }

	callGrails(grailsHome, testprojectRoot, 'dev', 'install-plugin') {
		ant.arg value: "functional-test $functionalTestPluginVersion"
	}

	callGrails(grailsHome, testprojectRoot, 'dev', 'install-plugin') {
		ant.arg value: pluginZip.absolutePath
	}

	// trigger plugin initialization
	callGrails(grailsHome, testprojectRoot, 'dev', 'compile')
}

private void copyProjectFiles() {

	ant.copy(todir: "$testprojectRoot/grails-app/controllers") {
		fileset(dir: projectfiles.path) {
			include name: '*Controller.groovy'
		}
	}

	ant.copy(todir: "$testprojectRoot/grails-app/services") {
		fileset(dir: projectfiles.path) {
			include name: '*Service.groovy'
		}
	}

	ant.copy file: "$projectfiles.path/LogEntry.groovy", todir: "$testprojectRoot/grails-app/domain"

	ant.copy file: "$projectfiles.path/Message.groovy", todir: "$testprojectRoot/src/groovy"
}

private void copyTests() {
	ant.copy(todir: "$testprojectRoot/test/functional") {
		fileset(dir: "$basedir/webtest/tests")
	}
}

private void deleteDir(String path) {
	if (new File(path).exists() && !deleteAll) {
		String code = "confirm.delete.$path"
		ant.input message: "$path exists, ok to delete?", addproperty: code, validargs: 'y,n,a'
		def result = ant.antProject.properties[code]
		if ('a'.equalsIgnoreCase(result)) {
			deleteAll = true
		}
		else if (!'y'.equalsIgnoreCase(result)) {
			printMessage "\nNot deleting $path"
			exit 1
		}
	}

	ant.delete dir: path
}

private void init(String name, config) {

	pluginVersion = config.pluginVersion
	if (!pluginVersion) {
		error "pluginVersion wasn't specified for config '$name'"
	}

	pluginZip = new File(basedir, "grails-cache-ehcache-${pluginVersion}.zip")
	if (!pluginZip.exists()) {
		error "plugin $pluginZip.absolutePath not found"
	}

	grailsHome = config.grailsHome
	if (!new File(grailsHome).exists()) {
		error "Grails home $grailsHome not found"
	}

	projectDir = config.projectDir
	appName = 'cache-ehcache-test-' + name
	testprojectRoot = "$projectDir/$appName"

	grailsVersion = config.grailsVersion
	dotGrails = config.dotGrails + '/' + grailsVersion
}

private void error(String message) {
	errorMessage "\nERROR: $message"
	exit 1
}

private void callGrails(String grailsHome, String dir, String env, String action, extraArgs = null) {
	ant.exec(executable: "$grailsHome/bin/grails", dir: dir, failonerror: 'true') {
		ant.env key: 'GRAILS_HOME', value: grailsHome
		ant.arg value: env
		ant.arg value: action
		extraArgs?.call()
	}
}

printMessage = { String message -> event('StatusUpdate', [message]) }
errorMessage = { String message -> event('StatusError', [message]) }

setDefaultTarget 'createCacheEhcacheTestApps'
