# Entity Summarization
Retrieve and summarize entity

##Coding Enviroment
- Ubuntu 16.04
- java 8
- Intellij 14.1.4



##How to run
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
  
