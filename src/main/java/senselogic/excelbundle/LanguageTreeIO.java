/*
 * Copyright 2006 Senselogic
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package senselogic.excelbundle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for reading and writing LanguagePacks to and from a source tree.
 *
 * @author Emil Eriksson
 * @version $Revision$
 */
public class LanguageTreeIO
{
    // Static --------------------------------------------------------
    private static Pattern pattern =
            Pattern.compile( "([^\\s=\\\\#!]+)\\s*=.*" );

    // Attributes ----------------------------------------------------
    private File root;
    private String refLang;
    private Set<String> languages = new HashSet<String>();

    private Map<String, BundleInfo> bundles =
            new LinkedHashMap<String, BundleInfo>();
    private Map<String, LanguagePack> packCache =
            new HashMap<String, LanguagePack>();
    private Map<String, LanguageFile> fileCache =
            new HashMap<String, LanguageFile>();

    // Constructors --------------------------------------------------
    /**
     * Constructs a new LanguageTreeIO.
     *
     * @param root    the root of the source tree to read language files from
     * @param refLang the language to use as reference for searching for
     *                bundles
     * @throws java.io.IOException
     */
    public LanguageTreeIO( File root, String refLang ) throws IOException
    {
        this.root = root;
        this.refLang = refLang;
        findBundles( root );
    }

    // Public --------------------------------------------------------
    /**
     * Returns the root of this LanguageTreeIO.
     *
     * @return
     */
    public File getRoot()
    {
        return root;
    }

    /**
     * Returns a Collection of all the languages that are available in at least
     * one bundle.
     *
     * @return
     */
    public Collection<String> getAvailableLanguages()
    {
        return languages;
    }

    /**
     * Returns the BundleInfo with the specified path.
     *
     * @param path
     * @return
     */
    public BundleInfo getBundle( String path )
    {
        return bundles.get( path );
    }

    /**
     * Returns a Collection of BundleInfo objects describing all of the
     * available bundles.
     *
     * @return
     */
    public Collection<BundleInfo> getBundles()
    {
        return bundles.values();
    }

    /**
     * Creates a LanguageFile from a file.
     *
     * @param bundlePath the relative logical path to the bundle, e.g.
     *                   bla/bla/mybundle
     * @param language   the language of the file
     * @return
     * @throws java.io.IOException
     */
    public LanguageFile loadLanguageFile( String bundlePath, String language )
            throws IOException
    {
        LanguageFile langFile = fileCache.get(
                LanguageFile.getFilename( bundlePath, language ) );
        if (langFile != null)
            return langFile;

        langFile = new LanguageFile( bundlePath, language );
        Properties prop = new CustomProperties();
        File file = new File( root, langFile.getFilename() );
        if (!file.exists())
            return null;

        InputStream inputStream = null;
        try
        {
            inputStream = new BufferedInputStream(new FileInputStream( file ));
            prop.load( inputStream );
        } finally
        {
            if (inputStream != null) inputStream.close();
        }

        for (Map.Entry<Object, Object> entry : prop.entrySet())
            langFile.setValue( (String) entry.getKey(), (String) entry.getValue() );

        fileCache.put( langFile.getFilename(), langFile );
        return langFile;
    }

    /**
     * Loads all the language files associated with the specified language and
     * returns them as a LanguagePack. This method caches the read file so
     * subsequent calls to this method will not read the file multiple times.
     *
     * @param language
     * @return
     * @throws java.io.IOException
     */
    public LanguagePack loadLanguage( String language ) throws IOException
    {
        LanguagePack pack = packCache.get( language );
        if (pack != null)
            return pack;

        pack = new LanguagePack( language );
        for (BundleInfo bundle : bundles.values())
        {
            if (!bundle.getLanguages().contains( language ))
                continue;

            LanguageFile langFile = loadLanguageFile( bundle.getPath(), language );
            if (langFile == null)
                continue;

            pack.addLanguageFile( langFile );
        }

        packCache.put( pack.getLanguage(), pack );
        return pack;
    }

    /**
     * Writes the specified LanguageFile to the source tree.
     */
    public void save( LanguageFile langFile ) throws IOException
    {
        File file = new File( root, langFile.getFilename() );
        if (!file.exists())
        {
            Properties prop = new Properties();
            for (LanguageFile.KeyValuePair pair : langFile.getPairs())
                prop.put( pair.getKey(), pair.getValue() );

            OutputStream outputStream = null;
            try
            {
                outputStream = new BufferedOutputStream(new FileOutputStream( file ));
                prop.store( outputStream, null );
            } finally
            {
                if (outputStream != null) outputStream.close();
            }
            return;
        }

        BufferedReader in = new BufferedReader( new InputStreamReader(
                new FileInputStream( file ), "ISO-8859-1" ) );
        List<String> lines = new ArrayList<String>();
        try
        {
            String line = null;
            while ((line = in.readLine()) != null)
                lines.add( line );
        }
        finally
        {
            in.close();
        }

        PrintStream out = new PrintStream(
                new FileOutputStream( file ), false, "ISO-8859-1" );
        try
        {
            //This is used to keep track of what keys remain to be written in
            //the end of the file
            Map<String, LanguageFile.KeyValuePair> remaining =
                    new LinkedHashMap<String, LanguageFile.KeyValuePair>();
            for (LanguageFile.KeyValuePair pair : langFile.getPairs())
                remaining.put( pair.getKey(), pair );

            for (String line : lines)
            {
                if ((line.trim().length() == 0) ||
                        (line.trim().charAt( 0 ) == '#') ||
                        (line.trim().charAt( 0 ) == '!'))
                {
                    out.println( line );
                    continue;
                }

                Matcher matcher = pattern.matcher( line.trim() );
                if (matcher.matches())
                {
                    String key = matcher.group( 1 );
                    String value = langFile.getValue( key );
                    if (value == null)
                        continue;

                    out.print( key );
                    out.print( " = " );
                    out.println( EscapeUtil.escape( value, false ) );
                    remaining.remove( key );
                }
            }

            //Let's write the remaining keys:
            for (LanguageFile.KeyValuePair pair : remaining.values())
            {
                out.print( pair.getKey() );
                out.print( " = " );
                out.println( EscapeUtil.escape( pair.getValue(), false ) );
            }
        }
        finally
        {
            out.close();
        }
    }

    /**
     * Returns true if the specified LanguageFile exists in the source tree,
     * that is, if it has a file with the same name as this one, not
     * necessarily an identical file.
     */
    public boolean exists( LanguageFile langFile )
    {
        return new File( root, langFile.getFilename() ).exists();
    }

    /**
     * Updates the internal list of available bundles and the available
     * languages in the bundles and clears the cache.
     */
    public void update() throws IOException
    {
        clearCache();
        bundles.clear();
        findBundles( root );
    }

    /**
     * Clears the cache.
     */
    public void clearCache()
    {
        packCache.clear();
        fileCache.clear();
    }

    // Private -------------------------------------------------------
    private void findBundles( File dir ) throws IOException
    {
        for (File file : dir.listFiles())
        {
            if (file.isDirectory())
            {
                findBundles( file );
                continue;
            }

            String bundleName = null;
            String suffix = "_" + refLang + ".properties";
            //We use english language files as a reference for what properties
            //files actually are language files and not other stuff
            if (file.getName().endsWith( suffix ) &&
                    !file.isDirectory())
            {
                String filename = file.getName();
                final String bundle =
                        filename.substring( 0, filename.indexOf( "_" ) );
                bundleName = bundle;

                //Let's find all of the languages
                Collection<String> bundleLangs = new ArrayList<String>();
                File[] sisterFiles = dir.listFiles( new FilenameFilter()
                {
                    public boolean accept( File path, String filename )
                    {
                        return filename.matches( bundle + ".*\\.properties" );
                    }
                } );
                for (File langFile : sisterFiles)
                {
                    filename = langFile.getName();
                    String language = null;
                    if (filename.contains( "_" ))
                    {
                        language = filename.substring( (
                                bundle.length() + 1),
                                filename.length() - ".properties".length() );
                    } else // Must be default language if it conatains no "_"
                        language = LanguageFile.DEFAULT_LANGUAGE;

                    bundleLangs.add( language );
                    languages.add( language );
                }

                //Now let's add a BundleInfo
                String relativePath =
                        dir.getCanonicalPath().substring(
                                root.getCanonicalPath().length() ) + File.separator;
                String completePath = relativePath + bundleName;
                bundles.put(
                        completePath,
                        new BundleInfo( completePath, bundleLangs ) );
            }
        }
    }
}
