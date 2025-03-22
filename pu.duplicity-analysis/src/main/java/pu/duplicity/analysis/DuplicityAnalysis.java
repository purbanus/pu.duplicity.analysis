package pu.duplicity.analysis;

import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pu.services.FileWalker;
import pu.services.Globals;
import pu.services.MatrixFormatter;
import pu.services.StringHelper;

public class DuplicityAnalysis
{
public static final Map<String, String> ENV = System.getenv();
public static final String USER = System.getenv( "USER" );
public static final String ROOT = "/media/" + USER + "/8TB#2/backups/pu2022/dejadup backups";
public static final String REGEX_PATTERN = "\\d{8}T\\d{6}Z";
public static final String TO_REGEX_PATTERN = "\\d{8}T\\d{6}Z.to.\\d{8}T\\d{6}Z";

private static final Pattern PATTERN = Pattern.compile( REGEX_PATTERN );
private static final Pattern TO_PATTERN = Pattern.compile( TO_REGEX_PATTERN );
private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss" );

public static class FileInfo
{
private final String type;
private int numberOfFiles;
private long totalSize;
public FileInfo( String aType )
{
	super();
	type = aType;
	numberOfFiles = 0;
	totalSize = 0;
}
@Override
public String toString()
{
	return "FileInfo [type=" + type + ", numberOfFiles=" + numberOfFiles + ", totalSize=" + totalSize + "]";
}
}
public static void main( String [] args )
{
	new DuplicityAnalysis().run();
}
public void run()
{
	File file = new File( ROOT );
	if ( ! file.exists() )
	{
		System.err.println( "Directory " + ROOT + " bestaat niet" );
		System.exit( 10 );
	}
	if ( ! file.isDirectory() )
	{
		System.err.println( ROOT + " is geen directory" );
		System.exit( 10 );
	}
	List<File> files = runFileWalker();
	Map<String, List<File>> dateMap = createDuplicityEntries( files );
	//printDuplicityDates( dateMap );
	Map<String, List<FileInfo>> fileInfoMap = createDuplicityFileTypes( dateMap );
	printFileInfos( fileInfoMap );
}
@SuppressWarnings( "deprecation" )
private List<File> runFileWalker()
{
	FileWalker fileWalker = new FileWalker( new File( ROOT ) );

	final List<File> ret = new ArrayList<>();
	fileWalker.addObserver( new Observer()
	{
		@Override
		public void update( Observable aObservable, Object aObject )
		{
			// aObject is a File
			ret.add( (File) aObject );
		}
	});
	fileWalker.run();
	return ret;
}
public Map<String, List<File>> createDuplicityEntries( List<File> aFiles )
{
	Map<String, List<File>> entries = new TreeMap<>();
	for ( File file : aFiles )
	{
		Optional<String> optionalDate = parseDate( file );
		if ( optionalDate.isPresent() )
		{
			String date = optionalDate.get();
			List<File> fileList = entries.get( date );
			if ( fileList == null )
			{
				fileList = new ArrayList<>();
				entries.put( date, fileList );
			}
			fileList.add( file );
		}
	}
	return entries;
}
public Optional<String> parseDate( File aFile )
{
	Matcher matcher = TO_PATTERN.matcher( aFile.getName() );
	if ( matcher.find() )
	{
		String date = aFile.getName().substring( matcher.start(), matcher.end() );
		return Optional.of( date );
	}
	matcher = PATTERN.matcher( aFile.getName() );
	if ( matcher.find() )
	{
		String date = aFile.getName().substring( matcher.start(), matcher.end() );
		return Optional.of( date );
	}
	System.out.println( "Geen datum gevonden: " + aFile.getName() );
	return Optional.empty();
}
@SuppressWarnings( "unused" )
private void printDuplicityDates( Map<String, List<File>> aDateMap )
{
	for ( String key : aDateMap.keySet() )
	{
		System.out.println( key + " contains " + aDateMap.get( key ).size() );
	}
}

public Map<String, List<FileInfo>> createDuplicityFileTypes( Map<String, List<File>> aDateMap )
{
	Map<String, List<FileInfo>> fileInfos = new TreeMap<>(); 
	for ( String key : aDateMap.keySet() )
	{
		Map<String, FileInfo> fileInfosPerFileType = analyzeFiles( aDateMap.get( key ) );
		List<FileInfo> fileInfoList = fileInfos.get( key );
		if ( fileInfoList == null )
		{
			fileInfoList = new ArrayList<>();
		}
		for ( String  fileType : fileInfosPerFileType.keySet() )
		{
			fileInfoList.add( fileInfosPerFileType.get(  fileType ) );
		}
		fileInfos.put( key, fileInfoList );
	}
	return fileInfos;
}
private Map<String, FileInfo> analyzeFiles( List<File> aFiles )
{
	Map<String, FileInfo> fileInfosPerFileType = new HashMap<>();
	for ( File file : aFiles )
	{
		String fileType = determineFileType( file );
		FileInfo fileInfo = fileInfosPerFileType.get( fileType );
		if ( fileInfo == null )
		{
			fileInfo = new FileInfo( fileType );
			fileInfosPerFileType.put( fileType, fileInfo );
		}
		fileInfo.numberOfFiles++;
		fileInfo.totalSize += file.length();
	}
	return fileInfosPerFileType;
}
private String determineFileType( File aFile )
{
	String fileName = aFile.getName();
	if ( fileName.contains( "duplicity-full-signatures" ) )
	{
		return "full-signatures";
	}
	else if ( fileName.contains( "duplicity-full" ) )
	{
		if ( fileName.contains( "manifest" ) )
		{
			return "full-manifest";
		}
		else
		{
			return "full";
		}
	}
	else if ( fileName.contains( "duplicity-inc" ) )
	{
		if ( fileName.contains( "manifest" ) )
		{
			return "inc-manifest";
		}
		else
		{
			return "inc";
		}
	}
	else if ( fileName.contains( "duplicity-new-signatures" ) )
	{
		return "new-signatures";
	}
	else
	{
		System.err.println( "Onbekend filetype: " + fileName );
		return "unknown";
	}
}
public void printFileInfos( Map<String, List<FileInfo>> fileInfoMap )
{
	NumberFormat numberFormat = Globals.getBigDecimalFormatter( 15,0 );
	numberFormat.setParseIntegerOnly( true );
	numberFormat.setGroupingUsed( true );

	String streep = StringHelper.repString( "-", 80 );
	MatrixFormatter matrixFormatter = new MatrixFormatter();
	matrixFormatter.setAlignment( 3, MatrixFormatter.ALIGN_RIGHT );
	matrixFormatter.setAlignment( 4, MatrixFormatter.ALIGN_RIGHT );
	matrixFormatter.addHeader( streep );
	matrixFormatter.addDetail( new String [] { "Date/Time", "FileType", "Number", "TotalSize" } );
	matrixFormatter.addHeader( streep ); 
	
	for ( String date : fileInfoMap.keySet() )
	{
		String formattedDateTime = createDates( date );
		
		for ( FileInfo fileInfo : fileInfoMap.get( date ) )
		{
			String numberOfFiles = numberFormat.format( fileInfo.numberOfFiles );
			String totalSize = numberFormat.format( fileInfo.totalSize );
			matrixFormatter.addDetail( new String [] { formattedDateTime, fileInfo.type, numberOfFiles, totalSize } );
			formattedDateTime = "";
		}
	}
	System.out.println( matrixFormatter.getOutput() );
}
public String createDates( String date )
{
	// De eerste datum
	LocalDateTime localDateTime = createDateTime( date );
	String formattedDateTime = localDateTime.format( DATE_TIME_FORMATTER );
	
	// Eventueel tweede datum
	if ( date.contains( ".to." ) )
	{
		LocalDateTime secondDateTime = createDateTime( date.substring( 20 ) );
		String formattedSecondDateTime = secondDateTime.format( DATE_TIME_FORMATTER );
		return formattedDateTime + " to " + formattedSecondDateTime;
	}
	return formattedDateTime;
}
public LocalDateTime createDateTime( String aDate )
{
	// Remove the T and the Z
	String dateWithout = aDate.substring( 0, 8 ) + aDate.substring( 9, 15 );
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	LocalDateTime dateTime = LocalDateTime.parse( dateWithout, formatter);
	return dateTime;
}
}
