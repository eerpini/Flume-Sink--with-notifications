all:
	make -C ./lib	
	ant 
clean:
	rm -rf ./build 
	rm -rf ./lib/build
	rm -rf ./notified_escaped_custom_dfs_plugin.jar
	rm -rf ./lib/notify.jar
