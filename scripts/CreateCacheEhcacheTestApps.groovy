includeTargets << grailsScript('_GrailsBootstrap')

functionalTestPluginVersion = '1.2.7'
projectfiles = new File(basedir, 'webtest/projectFiles')
grailsHome = null
grailsVersion = null
dotGrails = null
projectDir = null
appName = null
pluginVersion = null
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
		generateFiles()
	}
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
}

private void createApp() {

	ant.mkdir dir: projectDir

	deleteDir testprojectRoot

	callGrails grailsHome, projectDir, 'dev', 'create-app', [appName]
}

private void installPlugins() {

	File buildConfig = new File(testprojectRoot, 'grails-app/conf/BuildConfig.groovy')
	String contents = buildConfig.text
	contents = contents.replace('grails.project.class.dir = "target/classes"', "grails.project.work.dir = 'target'")
	contents = contents.replace('grails.project.test.class.dir = "target/test-classes"', '')
	contents = contents.replace('grails.project.test.reports.dir = "target/test-reports"', '')

	contents = contents.replace('//mavenLocal()', 'mavenLocal()')
	contents = contents.replace('grails.project.fork', 'grails.project.forkDISABLED')

	contents = contents.replace('plugins {', """plugins {
		test ":cache-ehcache:$pluginVersion"
		test ":functional-test:$functionalTestPluginVersion"
""")

	buildConfig.withWriter { it.writeLine contents }

	File configGroovy = new File(testprojectRoot, 'grails-app/conf/Config.groovy')
	contents = configGroovy.text
	contents += '''

grails.cache.config = {
	defaults {
		eternal false
		overflowToDisk false
		maxElementsOnDisk 0
	}
	cache {
		name 'message'
	}
	cache {
		name 'book'
	}
}

'''

	configGroovy.withWriter { it.writeLine contents }

	callGrails grailsHome, testprojectRoot, 'dev', 'compile', null, true // can fail when installing the functional-test plugin
	callGrails grailsHome, testprojectRoot, 'dev', 'compile'
}

private void copyProjectFiles() {

	ant.copy(todir: "$testprojectRoot/grails-app/conf", overwrite: true) {
		fileset(dir: projectfiles.path) {
			include name: 'DataSource.groovy'
			include name: 'UrlMappings.groovy'
		}
	}

	ant.copy(todir: "$testprojectRoot/grails-app/controllers") {
		fileset(dir: projectfiles.path) {
			include name: '*Controller.groovy'
		}
	}

	ant.copy(todir: "$testprojectRoot/grails-app/domain") {
		fileset(dir: projectfiles.path) {
			include name: 'Book.groovy'
			include name: 'LogEntry.groovy'
		}
	}

	ant.copy(todir: "$testprojectRoot/grails-app/services") {
		fileset(dir: projectfiles.path) {
			include name: '*Service.groovy'
		}
	}

	ant.copy(todir: "$testprojectRoot/grails-app/views") {
		fileset dir: "$projectfiles.path/gsp"
	}

	ant.copy(todir: "$testprojectRoot/src/groovy") {
		fileset file: "$projectfiles.path/Message.groovy"
	}
}

private void copyTests() {
	ant.copy(todir: "$testprojectRoot/test/functional") {
		fileset(dir: "$basedir/webtest/tests")
	}
}

private void generateFiles() {
	callGrails grailsHome, testprojectRoot, 'dev', 'generate-all', ['Book']

	ant.delete file: "$testprojectRoot/test/unit/BookControllerTests.groovy"

	File bookController = new File(testprojectRoot, 'grails-app/controllers/BookController.groovy')
	String contents = bookController.text

	contents = contents.replace('class BookController {', '''\
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable

class BookController extends AbstractCacheController {''')

	contents = contents.replace('def list()', '@Cacheable("book") def list()')
	contents = contents.replace('def show()', '@Cacheable("book") def show()')
	contents = contents.replace('def save()', '@CacheEvict(value="book", allEntries=true) def save()')
	contents = contents.replace('def update()', '@CacheEvict(value="book", allEntries=true) def update()')
	contents = contents.replace('def delete()', '@CacheEvict(value="book", allEntries=true) def delete()')

	bookController.withWriter { it.writeLine contents }
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

private void error(String message) {
	errorMessage "\nERROR: $message"
	exit 1
}

private void callGrails(String grailsHome, String dir, String env, String action, List extraArgs = null, boolean ignoreFailure = false) {

	String resultproperty = 'exitCode' + System.currentTimeMillis()
	String outputproperty = 'execOutput' + System.currentTimeMillis()

	println "Running 'grails $env $action ${extraArgs?.join(' ') ?: ''}'"

	ant.exec(executable: "${grailsHome}/bin/grails", dir: dir, failonerror: false,
				resultproperty: resultproperty, outputproperty: outputproperty) {
		ant.env key: 'GRAILS_HOME', value: grailsHome
		ant.arg value: env
		ant.arg value: action
		extraArgs.each { ant.arg value: it }
		ant.arg value: '--stacktrace'
	}

	println ant.project.getProperty(outputproperty)

	int exitCode = ant.project.getProperty(resultproperty) as Integer
	if (exitCode && !ignoreFailure) {
		exit exitCode
	}
}


printMessage = { String message -> event('StatusUpdate', [message]) }
errorMessage = { String message -> event('StatusError', [message]) }

setDefaultTarget 'createCacheEhcacheTestApps'
