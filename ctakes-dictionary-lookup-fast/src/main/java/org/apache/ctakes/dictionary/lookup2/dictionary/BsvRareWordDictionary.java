/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.dictionary.lookup2.dictionary;

import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;
import org.apache.ctakes.dictionary.lookup2.util.LookupUtil;
import org.apache.ctakes.dictionary.lookup2.util.collection.CollectionMap;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * A RareWordDictionary created from a bar-separated value (BSV) file.  The file can have 2 or 3 columns,
 * in the format CUI|TEXT or CUI|TUI|TEXT.  The text will be tokenized and rare word indexing done automatically for
 * internal storage and retrieval.  If TUI is not supplied then CUI duplicates as TUI.
 * This dictionary is really just a wrapper of a {@link MemRareWordDictionary} with a file reader.
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/9/14
 */
final public class BsvRareWordDictionary implements RareWordDictionary {

   static private final Logger LOGGER = Logger.getLogger( "BsvRareWordDictionary" );

   static private final String BSV_FILE_PATH = "bsvPath";

   private RareWordDictionary _delegateDictionary;


   public BsvRareWordDictionary( final String name, final UimaContext uimaContext, final Properties properties ) {
      this( name, properties.getProperty( BSV_FILE_PATH ) );
   }


   public BsvRareWordDictionary( final String name, final String bsvFilePath ) {
      this( name, new File( bsvFilePath ) );
   }

   public BsvRareWordDictionary( final String name, final File bsvFile ) {
      final Collection<RareWordTermMapCreator.CuiTerm> cuiTerms = parseBsvFile( bsvFile );
      final CollectionMap<String, RareWordTerm> rareWordTermMap
            = RareWordTermMapCreator.createRareWordTermMap( cuiTerms );
      _delegateDictionary = new MemRareWordDictionary( name, rareWordTermMap );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegateDictionary.getName();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final FastLookupToken fastLookupToken ) {
      return _delegateDictionary.getRareWordHits( fastLookupToken );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordTerm> getRareWordHits( final String rareWordText ) {
      return _delegateDictionary.getRareWordHits( rareWordText );
   }


   /**
    * Create a collection of {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordTermMapCreator.CuiTerm} Objects
    * by parsing a bsv file.  The file can be in one of two columnar formats:
    * <p>
    * CUI|Text
    * </p>
    * or
    * <p>
    * CUI|TUI|Text
    * </p>
    * or
    * <p>
    * CUI|TUI|Text|PreferredTerm
    * </p>
    * If the TUI column is omitted then the entityId for the dictionary is used as the TUI
    *
    * @param bsvFile file containing term rows and bsv columns
    * @return collection of all valid terms read from the bsv file
    */
   static private Collection<RareWordTermMapCreator.CuiTerm> parseBsvFile( final File bsvFile ) {
      final Collection<RareWordTermMapCreator.CuiTerm> cuiTerms = new ArrayList<>();
      try {
         final BufferedReader reader = new BufferedReader( new FileReader( bsvFile ) );
         String line = reader.readLine();
         while ( line != null ) {
            if ( line.startsWith( "//" ) || line.startsWith( "#" ) ) {
               continue;
            }
            final String[] columns = LookupUtil.fastSplit( line, '|' );
            final RareWordTermMapCreator.CuiTerm cuiTerm = createCuiTuiTerm( columns );
            if ( cuiTerm != null ) {
               // Add to the dictionary
               cuiTerms.add( cuiTerm );
            } else {
               LOGGER.warn( "Bad BSV line " + line + " in " + bsvFile.getPath() );
            }
            line = reader.readLine();
         }
         reader.close();
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      return cuiTerms;
   }

   /**
    * @param columns two or three columns representing CUI,Text or CUI,TUI,Text respectively
    * @return a term created from the columns or null if the columns are malformed
    */
   static private RareWordTermMapCreator.CuiTerm createCuiTuiTerm( final String[] columns ) {
      if ( columns.length < 2 ) {
         return null;
      }
      final int cuiIndex = 0;
      int termIndex = 1;
      if ( columns.length >= 3 ) {
         // second column is a tui, so text is in the third column
         termIndex = 2;
      }
      if ( columns[cuiIndex].trim().isEmpty() || columns[termIndex].trim().isEmpty() ) {
         return null;
      }
      final String cui = columns[cuiIndex];
      final String term = columns[termIndex].trim().toLowerCase();
      return new RareWordTermMapCreator.CuiTerm( cui, term );
   }

}
