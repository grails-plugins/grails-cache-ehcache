class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?" {}

		"/withParams/$foo/$bar" {
			controller = 'test'
			action = 'withParams'
		}

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
