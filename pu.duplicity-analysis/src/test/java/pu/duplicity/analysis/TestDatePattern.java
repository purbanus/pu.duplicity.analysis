package pu.duplicity.analysis;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class TestDatePattern
{
public static final String ROOT = "/media/$USER/8TB#2/backups/pu2022/dejadup backups";
public static final String REGEX_PATTERN = "\\d{8}T\\d{6}Z";
//     \\d{8}T\\d{6}Z

private static final Pattern PATTERN = Pattern.compile( REGEX_PATTERN );

@Test
public void testDatePattern()
{
	String fileName = "duplicity-full.20230317T182941Z.vol21.difftar.gz";
	Matcher matcher = PATTERN.matcher( fileName );
	assertTrue( matcher.find() );
	assertEquals( 15, matcher.start() );
	assertEquals( 31, matcher.end() );
	assertEquals( "20230317T182941Z", fileName.substring( matcher.start(), matcher.end() ) );
}
}
