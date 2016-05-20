module.exports = {
	isEnabled : function(success, failure){
		cordova.exec(success, failure, 'FincludePrinter', 'isEnabled', []);
	},
	printText : function(text, success, failure){
		cordova.exec(success, failure, 'FincludePrinter', 'printText', [text]);
	}
}
