package net.ar90n.logcatvisor;

interface ILoggerService
{
	void startLogger();
	void stopLogger();
	void setOppacity( int oppacity );
	void setHorizontalLogLinesWeight( int weight );
	void setVerticalLogLinesWeight( int weight );
	void setHorizontalLogLinesOffset( int weight );
	void setVerticalLogLinesOffset( int weight );
	void setFontsize( int size );
	void setFontColor( int color );
	void setSelectedContents( int contents );
	void setGrepText( String text );
	void enableGrep( boolean flag );
}