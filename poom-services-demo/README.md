# Demo application : a digital video club !



 curl -kv http://localhost:8888/demo/late-rental-tasks | json_pp
 curl -XPOST -kv http://localhost:8888/demo/late-rental-tasks | json_pp
 curl -kv http://localhost:8888/demo/late-rental-tasks | json_pp
 curl -kv http://localhost:8888/demo/NETFLIX/movies/4438180b-09fa-4d86-a9f9-623e92b45dca/rentals | json_pp
 curl -kv http://localhost:8888/demo/NETFLIX/movies | json_pp
 curl -kv http://localhost:8888/demo/NETFLIX/movies?filter=title%20%3D%3D%20%27Psycho%27 | json_pp
 curl -kv http://localhost:8888/demo/NETFLIX/movies/60549690-680d-4c12-8663-14e35282701a | json_pp
 curl -kv http://localhost:8888/demo/NETFLIX/movies/60549690-680d-4c12-8663-14e35282701a/rentals | json_pp
 curl -kv http://localhost:8888/demo/late-rental-tasks | json_pp
 curl -XPOST -kv http://localhost:8888/demo/late-rental-tasks | json_pp
 curl -kv http://localhost:8888/demo/late-rental-tasks | json_pp
 curl -kv http://localhost:8888/demo/NETFLIX/movies/60549690-680d-4c12-8663-14e35282701a/rentals | json_pp
 curl -kv http://localhost:8888/demo/NETFLIX/customers/picsou/rentals | json_pp
 curl -kv http://localhost:8888/demo/NETFLIX/movies/71f898b0-e96f-4657-a6d0-2008b4b3d045/rentals/a2de8dcc-b519-46b6-bc5b-638be4e95eba | json_pp
 curl -XPOST -kv http://localhost:8888/demo/NETFLIX/movies/71f898b0-e96f-4657-a6d0-2008b4b3d045/rentals --data '{\"customer\":\"toto\"}' | json_pp
 curl -XPOST -kv http://localhost:8888/demo/NETFLIX/movies/71f898b0-e96f-4657-a6d0-2008b4b3d045/rentals --data '{"customer":"toto"}' | json_pp
 curl -kv http://localhost:8888/demo/NETFLIX/movies/71f898b0-e96f-4657-a6d0-2008b4b3d045/rentals/a2de8dcc-b519-46b6-bc5b-638be4e95eba | json_pp
 curl -XPATCH -kv http://localhost:8888/demo/NETFLIX/movies/71f898b0-e96f-4657-a6d0-2008b4b3d045/rentals/a2de8dcc-b519-46b6-bc5b-638be4e95eba --data '{"type":"RETURN"}' | json_pp
 curl -XPOST -kv http://localhost:8888/demo/NETFLIX/movies/71f898b0-e96f-4657-a6d0-2008b4b3d045/rentals --data '{"customer":"toto"}' | json_pp
