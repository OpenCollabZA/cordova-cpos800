module.exports = {
	readTagId : function(success, failure){
		cordova.exec(success, failure, 'CPOS800', 'readTagId', []);
	}
};
