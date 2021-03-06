package com.quicklyric.romanizer;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import org.acra.ACRA;

import java.util.List;

import me.xuender.unidecode.Unidecode;

/**
 * This file is part of QuickLyricRomanizer
 * Created by geecko on 1/07/17.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class RomanizerUtil {

    private static Tokenizer tokenizer;
    private static boolean profileLoaded = false;

    static String romanize(String s, List<String> profiles) {
        if (containsJapanese(s, profiles)) {
            boolean isInBrackets = false;
            if (tokenizer == null) {
                try {
                    tokenizer = new Tokenizer.Builder().build();
                } catch (Exception e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleSilentException(e);
                }
            }
            List<Token> list = tokenizer.tokenize(s);
            StringBuilder builder = new StringBuilder();
            for (Token token : list) {
                if (token.getSurface().contains("]")) {
                    builder.append(token.getSurface());
                    isInBrackets = false;
                    continue;
                } else if (token.getSurface().contains("[")) {
                    builder.append(token.getSurface());
                    isInBrackets = true;
                    continue;
                }
                if (isInBrackets) {
                    builder.append(token.getSurface());
                    continue;
                }
                try {
                    builder.append(token.isKnown() ? token.getReading() : token.getSurface());
                } catch (ArrayIndexOutOfBoundsException e) {
                    builder.append(token.getSurface());
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleSilentException(e);
                }
                if (!(token.getSurface().equals("<") || token.getSurface().equals("br") || token.getSurface().equals(">"))) {
                    if (!(builder.toString().endsWith(" ") || builder.toString().endsWith("\n")))
                        builder.append(" ");
                }
            }
            s = builder.toString().trim();
            s = s.replaceAll("\\s+!", "!").replaceAll("\\s+\\?", "?").replaceAll("\\s+:", ":");
        }
        String output = addSpacesBeforeUppercase(Unidecode.decode(s));
        return output.replaceAll(" +", " ");
    }

    public static boolean detectIdeographic(String s) {
        for (int i = 0; i < s.length(); ) {
            int codepoint = s.codePointAt(i);
            i += Character.charCount(codepoint);
            if (isIdeographic(codepoint)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIdeographic(int codepoint) {
        return isCJK(codepoint) || isJapanese(codepoint) || isKoreanHangul(codepoint);
    }

    private static boolean isCJK(int codepoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codepoint);
        return (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block) ||
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block) ||
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B.equals(block) ||
                Character.UnicodeBlock.CJK_COMPATIBILITY.equals(block) ||
                Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS.equals(block) ||
                Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block) ||
                Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT.equals(block) ||
                Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT.equals(block) ||
                Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION.equals(block) ||
                Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS.equals(block) ||
                Character.UnicodeBlock.KANGXI_RADICALS.equals(block) ||
                Character.UnicodeBlock.IDEOGRAPHIC_DESCRIPTION_CHARACTERS.equals(block));
    }

    private static boolean containsJapanese(String str, List<String> profiles) {
        for (int i = 0; i < str.toCharArray().length; i++) {
            if (isJapanese(str.codePointAt(i)))
                return true;
        }

        if (!profileLoaded) {
            try {
                DetectorFactory.loadProfile(profiles);
                profileLoaded = true;
            } catch (Exception ignored) {
            }
        }

        Detector detector;
        try {
            detector = DetectorFactory.create();
        } catch (LangDetectException e) {
            e.printStackTrace();
            return false;
        }
        detector.append(str);
        try {
            return "ja".equals(detector.detect());
        } catch (LangDetectException e) {
            return false;
        }
    }

    private static boolean isJapanese(int codepoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codepoint);
        return (Character.UnicodeBlock.HIRAGANA.equals(block) ||
                Character.UnicodeBlock.KATAKANA.equals(block) ||
                Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS.equals(block));
    }

    private static boolean isKoreanHangul(int codepoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codepoint);
        return (Character.UnicodeBlock.HANGUL_JAMO.equals(block) ||
                Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO.equals(block) ||
                Character.UnicodeBlock.HANGUL_SYLLABLES.equals(block));
    }

    private static String addSpacesBeforeUppercase(String s) {
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            int codepoint = Character.codePointAt(s, i);
            if (Character.isUpperCase(c) && !Character.isSpaceChar(s.charAt(i - Character.charCount(codepoint)))) {
                s = s.substring(0, i) + ' ' + s.substring(i, s.length());
                i++;
            }
        }
        return s;
    }

    static boolean areProfilesLoaded() {
        return profileLoaded;
    }
}