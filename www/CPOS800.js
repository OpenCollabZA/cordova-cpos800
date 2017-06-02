module.exports = {
	/**
	  * Error code when there was a timeout waiting for a tag to be read.
	  */
	ERROR_TIMEOUT : 1,

	/**
	  * Error code when the user cancelled reading a tag while we where still waiting
	  * for a tag.
	  */
	ERROR_CANCELLED : 2,

	/**
	  * IO Exception trying to read card id.
	  */
	ERROR_IO : 3,
	
	getCardId : function(successCallback, errorCallback){
		cordova.exec(successCallback, errorCallback, "CPOS800Plugin", "getCardId", []);
	},
	cancelReadTagId : function(successCallback, errorCallback){
		cordova.exec(successCallback, errorCallback, "CPOS800Plugin", "cancelReadTagId", []);
	}
};
