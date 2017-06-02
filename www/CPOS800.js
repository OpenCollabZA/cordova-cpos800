module.exports = {
	getCardId : function(successCallback, errorCallback){
		cordova.exec(successCallback, errorCallback, "CPOS800Plugin", "getCardId", []);
	},
	cancelReadTagId : function(successCallback, errorCallback){
		cordova.exec(successCallback, errorCallback, "CPOS800Plugin", "cancelReadTagId", []);
	}
};
