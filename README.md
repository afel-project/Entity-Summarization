# Entity Summarization
Retrieve and summarize entity

##Coding Enviroment
- Ubuntu 16.04
- java 8
- Intellij 14.1.4

## Installation

To setup the API follow these steps:

```js
> git clone https://github.com/ran-yu/entitySummarization.git
> cd entitySummarization
> mvn compile
> mvn war:war
```
This will build the war file in the target directory.
Copy the war into the deployment directory of your installed Java Servlet (e.g. apache-tomcat/webapps/) container.

##Methods
The individual methods are accessible through URL once the REST API setup succeeded. 

The URL is formatted as: http://tmocatserver.xxx.yyy.de:8080/es-1.0/ES/entitySum?type=Movie&query=Forrest%20Gump&disambiguation=http://dbpedia.org/resource/Forrest_Gump

###Query the summary for an entity:
Method name: entitySum

Parameters: {type, query, disambiguation}

Output: A string in JASON format as the example in the end of this readme.

##Entity Summarization class usage
###1. Creat an object:  
```js
> EntitySummarization es = new EntitySummarization();
```
###2. Load index: 
```js
> es.load_index("Movie");
```
###3. Summarize Given query
```js
//parameters  @1: query term, e.g. "Forrest Gump"
//            @2: query type, e.g. "Movie" , this need to be one of the loaded index types
//            @3: disambiguation page url, e.g. the corresponding DBpedia page "http://dbpedia.org/resource/Forrest_Gump"
> String res = es.summarize("Forrest Gump", "Movie", "http://dbpedia.org/resource/Forrest_Gump");
```

##Result

The returned string "res" is in json format, e.g.
```js
{
	"facts": [
		{
			"predicate": "name",
			"object": "Forrest Gump"
		},
		{
			"predicate": "genre",
			"object": "Romance"
		},
		{
			"predicate": "awards",
			"object": "Won 6 Oscars."
		}	]
}
```
  
