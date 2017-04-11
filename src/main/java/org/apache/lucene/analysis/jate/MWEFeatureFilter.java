package org.apache.lucene.analysis.jate;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Map;

/**
 * create features for a MWE to be used by supervised learning. Features are attached as payload
 *
 */
public final class MWEFeatureFilter extends TokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PayloadAttribute exitingPayload = addAttribute(PayloadAttribute.class);

    private WordShapeTagger wordShapeTagger = new WordShapeTagger();

    public MWEFeatureFilter(TokenStream input) {
        super(input);

    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            String tok = new String(termAtt.buffer(),0, termAtt.length());
            BytesRef payload = exitingPayload.getPayload();
            if(payload==null)
                return true;
            MWEMetadata metadata = MWEMetadata.deserialize(payload.utf8ToString());
            MWEMetadata features = new MWEMetadata();

            //step 1, copy over existing meta data
            for(Map.Entry<MWEMetadataType, String> e: metadata.metadata.entrySet()){
                features.addMetaData(e.getKey(), e.getValue());
            }

            //step 2, add orthographic features
            boolean hasNumber=wordShapeTagger.hasNumber(tok);
            boolean hasAcronym= wordShapeTagger.hasAcronym(tok);
            boolean hasUppercase=wordShapeTagger.hasUppercase(tok);
            boolean hasSymbol = wordShapeTagger.hasSymbol(tok);
            boolean hasDigit=wordShapeTagger.hasDigit(tok);
            features.addMetaData(MWEMetadataType.HAS_ACRONYM_TOKEN, String.valueOf(hasAcronym));
            features.addMetaData(MWEMetadataType.HAS_NUMERIC_TOKEN, String.valueOf(hasNumber));
            features.addMetaData(MWEMetadataType.HAS_UPPERCASE, String.valueOf(hasUppercase));
            features.addMetaData(MWEMetadataType.HAS_SYMBOL, String.valueOf(hasSymbol));
            features.addMetaData(MWEMetadataType.HAS_DIGIT, String.valueOf(hasDigit));

            exitingPayload.setPayload(new BytesRef(MWEMetadata.serialize(features)));
            termAtt.setEmpty().append(tok);
            return true;
        } else {
            return false;
        }
    }
}
