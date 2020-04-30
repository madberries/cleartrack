package pac.config;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pac.util.TaintUtils;

/**
 * This class handles the config file tokenization mechanisms.  By using a tokenizer, you have the ability
 * to check tokens relative to the current tainted region via regular expressions.
 * 
 * @author jeikenberry
 */
public class Tokenizer {
    private final Set<Character> tokens;

    /**
     * Hashmap mapping quoted characters designating token blocks to their respective escape character.
     */
    private final Map<Character, Character> defaultEscChars;
    private final Map<Character, Set<Character>> allEscChars;
    private final Map<Character, CharMapEntry> charEscapeMap;

    public Tokenizer() {
        tokens = new HashSet<Character>();
        defaultEscChars = new HashMap<Character, Character>();
        charEscapeMap = new HashMap<Character, CharMapEntry>();
        allEscChars = new HashMap<Character, Set<Character>>();
    }

    private void tokenize(Tokenized tokenized) throws IOException {
        final String stringToTokenize = tokenized.getString();
        int strLen = stringToTokenize.length();
        char blockChar = 0; // always unmapped in chartTaint
        char escapeChar = 0; // always unmapped in charTaint
        for (int tokenIdx = 0; tokenIdx < strLen; tokenIdx++) {
            Character c = stringToTokenize.charAt(tokenIdx);
            if (blockChar > 0) {
                int endIdx = Utils.nextTrustedQuote(stringToTokenize, blockChar, escapeChar, tokenIdx);
                if (endIdx < 0) {
                    tokenized.addTokenizedString(stringToTokenize.substring(tokenIdx), blockChar);
                    break;
                } else {
                    tokenized.addTokenizedString(stringToTokenize.substring(tokenIdx, endIdx + 1), blockChar);
                    tokenIdx = endIdx;
                }
                blockChar = 0;
            } else if (TaintUtils.characterIsWhitespace(c)) {
                tokenized.addTokenizedString((char) 0);
            } else if (TaintUtils.charSetContains(tokens, c)) {
                //  } else if (tokens.contains(c)) {
                tokenized.addTokenizedString(c, (char) 0);
                tokenized.setPosition(tokenIdx);
                tokenized.addTokenizedString((char) 0);
            } else if (TaintUtils.charMapContainsKey(defaultEscChars, c)) {
                //  } else if (defaultEscChars.containsKey(c)) {
                blockChar = c;
                escapeChar = defaultEscChars.get(c);
                tokenized.addTokenizedString(c, (char) 0);
                tokenized.setPosition(tokenIdx);
            } else {
                tokenized.appendToCurrentToken(tokenIdx, String.valueOf(c));
            }
        }
        tokenized.addTokenizedString((char) 0);
    }

    private void tokenizeTracked(Tokenized tokenized, NotifyMsg notifyMsg) throws IOException {
        String stringToTokenize = tokenized.getString();
        if (TaintUtils.isAllTainted(stringToTokenize, 0, stringToTokenize.length() - 1)) {
            notifyMsg.append("unable to tokenize entirely tainted data, so " + "treat this as an attack.");
            notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
            try {
                notifyMsg
                        .setExceptionConstructor(CleartrackException.class.getConstructor(new Class[] { String.class }));
            } catch (SecurityException e) {
            } catch (NoSuchMethodException e) {
            }
            Notify.notifyAndRespond(notifyMsg);
        }
        int strLen = stringToTokenize.length();
        char blockChar = 0; // in charTaint is always unmapped
        char escapeChar = 0; // in charTaint is always unmapped
        for (int tokenIdx = 0; tokenIdx < strLen; tokenIdx++) {
            char c = stringToTokenize.charAt(tokenIdx);
            boolean sameToken = false;
            if (blockChar > 0) {
                int endIdx = Utils.nextTrustedQuote(stringToTokenize, blockChar, escapeChar, tokenIdx);
                if (endIdx < 0) {
                    tokenized.addTokenizedString(TaintUtils.substring(stringToTokenize, tokenIdx), blockChar);
                    break;
                } else {
                    tokenized.addTokenizedString(TaintUtils.substring(stringToTokenize, tokenIdx, endIdx + 1),
                                                 blockChar);
                    tokenIdx = endIdx;
                }
                blockChar = 0;
            } else if (TaintUtils.isTrusted(stringToTokenize, tokenIdx, tokenIdx)) {
                if (TaintUtils.characterIsWhitespace(c)) {
                    tokenized.addTokenizedString((char) 0);
                } else if (TaintUtils.charSetContains(tokens, c))
                //  else if (tokens.contains(c))
                {
                    tokenized.addTokenizedString(c, (char) 0);
                    tokenized.setPosition(tokenIdx);
                    tokenized.addTokenizedString((char) 0);
                } else if (TaintUtils.charMapContainsKey(defaultEscChars, c))
                //  else if (defaultEscChars.containsKey(c))
                {
                    blockChar = c;
                    escapeChar = defaultEscChars.get(c);
                    tokenized.addTokenizedString(c, (char) 0);
                    tokenized.setPosition(tokenIdx);
                } else {
                    sameToken = true;
                }
            } else if (charEscapeMap.containsKey(c)) // Untrusted non-quote character that needs escaped properly
            {
                CharMapEntry charMapEntry = charEscapeMap.get(c);
                int action = charMapEntry.action;
                if (action == RunChecks.REPLACE_ACTION) {
                    String escapeSeq = charMapEntry.escapeSequence;
                    escapeSeq = TaintUtils.mark(escapeSeq, TaintUtils.taintAt(stringToTokenize, tokenIdx), 0,
                                                escapeSeq.length() - 1);
                    tokenized.appendToCurrentToken(tokenIdx, escapeSeq);
                    StringBuilder temp = TaintUtils.replace(TaintUtils.newStringBuilder(stringToTokenize), tokenIdx,
                                                            tokenIdx + 1, escapeSeq);
                    tokenIdx += temp.length() - stringToTokenize.length();
                    tokenized.setString(TaintUtils.toString(temp));
                    stringToTokenize = tokenized.getString();
                    strLen = stringToTokenize.length();
                } else {
                    tokenized.appendToCurrentToken(tokenIdx, c);
                }

                if (notifyMsg != null) {
                    notifyMsg.setAction(action);
                    notifyMsg.append("found a tainted special character '" + c + "' that needs to be escaped");
                    notifyMsg.append("\n");

                    if (action == RunChecks.REPLACE_ACTION) {
                        notifyMsg.append("Altered line to: \"");
                        notifyMsg.append(stringToTokenize);
                        notifyMsg.append("\"\n");
                    }

                    if (action == RunChecks.EXCEPTION_ACTION || action == RunChecks.TERMINATE_ACTION) {
                        notifyMsg.prepareForExceptionOrTerminate(charMapEntry.exception, action);
                    }
                    Notify.notifyAndRespond(notifyMsg);
                }
            } else {
                sameToken = true;
            }

            if (sameToken) {
                tokenized.appendToCurrentToken(tokenIdx, c);
            }
        }
        tokenized.addTokenizedString((char) 0);
    }

    /**
     * Tokenizes the input string using customized special token characters and token 
     * blocks (i.e quoted strings).
     * 
     * @param stringToTokenize - Input string to tokenize.
     * @return Tokenized object
     * @throws IOException 
     */
    public Tokenized tokenize(String stringToTokenize, NotifyMsg notifyMsg) throws IOException {
        boolean tracked = TaintUtils.isTracked(stringToTokenize);
        Tokenized tokenized = new Tokenized(stringToTokenize, tracked);
        if (tracked) {
            tokenizeTracked(tokenized, notifyMsg);
        } else {
            tokenize(tokenized);
        }
        return tokenized;
    }

    // Need this no-tracking tokenizer for when are expanding $ENV_NAME.
    // $ENV_NAME need to not be expanded when enclosed in single quotes, regardless
    // if the enclosing single quotes are trusted or untrusted.
    public Tokenized tokenizeNoTracking(String stringToTokenize, NotifyMsg notifyMsg) throws IOException {
        Tokenized tokenized = new Tokenized(stringToTokenize, false);
        tokenize(tokenized);
        return tokenized;
    }

    public void addCharacterMap(char c, String escapeSeq, int action, Constructor<?> exception) {
        CharMapEntry charMapEntry = new CharMapEntry();
        charMapEntry.escapeSequence = escapeSeq;
        charMapEntry.action = action;
        charMapEntry.exception = exception;
        charEscapeMap.put(c, charMapEntry);
    }

    /**
     * Adds special token characters to this tokenizer.
     * 
     * @param token character
     */
    public void addToken(char token) {
        tokens.add(token);
    }

    /**
     * Adds tokenized blocks (i.e. quoted and double quoted strings) to this 
     * tokenizer.
     * 
     * @param token character
     */
    public void addQuotedBlock(char token, char escapeToken) {
        Set<Character> altEscapes = allEscChars.get(token);
        if (altEscapes == null) {
            altEscapes = new HashSet<Character>();
            allEscChars.put(token, altEscapes);
            defaultEscChars.put(token, escapeToken);
        }
        if (escapeToken != 0)
            altEscapes.add(escapeToken);
    }

    public void buildTkn_parseLine(String line) throws MsgException {
        final String[] lineRay = line.split("\\s+"); // PatternSyntaxException

        if (lineRay[0].equals(ConfigFileTokens.TOKENS_TKN)) {
            char[] tkns = lineRay[1].trim().toCharArray();
            for (char t : tkns)
                tokens.add(t);
        } else if (lineRay[0].equals(ConfigFileTokens.QUOTED_BLOCK_TKN)) {
            char quoteChar = lineRay[1].trim().charAt(0);
            if (lineRay.length > 2) {
                for (int i = 1; i < lineRay.length; i++)
                    addQuotedBlock(quoteChar, lineRay[i].trim().charAt(0));
            } else {
                addQuotedBlock(quoteChar, (char) 0);
            }
        } else if (lineRay[0].equals(ConfigFileTokens.CHAR_MAP_TKN)) {
            // TODO: add checks

            Constructor<?> exception = (lineRay.length < 5) ? (null) : Utils.buildExceptionConstructor(lineRay[4]);

            addCharacterMap(lineRay[1].charAt(0), lineRay[2], RunChecks.actionStringToInt(lineRay[3]), exception);
        } else {
            throw new MsgException("Unknown token: " + lineRay[0]);
        }
    }

    static class Token {
        private final String token;
        private int startIdx, endIdx;
        private final char quoteChar;

        public Token(String tokenStr, int startPos, char quoteChar) {
            token = tokenStr;
            startIdx = startPos;
            endIdx = startPos + token.length() - 1;
            this.quoteChar = quoteChar;
        }

        /**
         * 
         * @return The quote character of the tokenized block (if quoted).  Otherwise the null character.
         */
        public char getQuoteChar() {
            return quoteChar;
        }

        public boolean contains(int pos) {
            return startIdx <= pos && endIdx >= pos;
        }

        @Override
        public String toString() {
            return token;
        }

        public int getStart() {
            return startIdx;
        }

        public int getEnd() {
            return endIdx;
        }

        public void shift(int offset) {
            startIdx += offset;
            endIdx += offset;
        }
    }

    public class Tokenized extends ArrayList<Token> {

        /**
         * 
         */
        private static final long serialVersionUID = -9002486119711984959L;

        private boolean tracked;
        private String stringToTokenize;
        private StringBuilder currentToken;
        private int position;

        private Tokenized(String stringToTokenize, boolean tracked) {
            this.tracked = tracked;
            this.stringToTokenize = stringToTokenize;
            currentToken = new StringBuilder();
        }

        private void setString(String string) {
            this.stringToTokenize = string;
        }

        public String getString() {
            return stringToTokenize;
        }

        private void appendToCurrentToken(int position, char c) throws IOException {
            int oldLen = currentToken.length();
            if (oldLen == 0)
                this.position = position;
            if (tracked) {
                int len = currentToken.length();
                TaintUtils.append(currentToken, c);
                TaintUtils.mark(currentToken, TaintUtils.taintAt(stringToTokenize, position), len, len);
            } else {
                currentToken.append(c);
            }
        }

        /* UNUSED
        private void appendToCurrentToken_chartaint(int position, char c) { 
            int oldLen = currentToken.length();
            if (oldLen == 0)
                this.position = position;
            if (tracked) {
                c = (CharMap.string_is_trusted_at(stringToTokenize, position) ?
                     CharMap.map_char(c) :
                     CharMap.unmap_char(c));
                currentToken.append(c);
            } else {
                currentToken.append(c);
            }
        }
        */

        private void appendToCurrentToken(int position, String str) {
            if (currentToken.length() == 0)
                this.position = position;
            if (tracked)
                TaintUtils.append(currentToken, str);
            else
                currentToken.append(str);
        }

        private void setPosition(int position) {
            this.position = position;
        }

        private void addTokenizedString(String str, char quoteChar) {
            if (tracked)
                TaintUtils.append(currentToken, str);
            else
                currentToken.append(str);
            addTokenizedString(quoteChar);
        }

        private void addTokenizedString(char quoteChar) {
            if (currentToken.length() == 0) {
                ;
            } else if (tracked) {
                add(new Token(TaintUtils.toString(currentToken), position, quoteChar));
            } else {
                add(new Token(currentToken.toString(), position, quoteChar));
            }
            currentToken = TaintUtils.newStringBuilder();
        }

        private void addTokenizedString(char lookahead, char quoteChar) throws IOException {
            if (tracked) {
                if (currentToken.length() == 0) {
                    currentToken = TaintUtils.newStringBuilder(TaintUtils
                            .toString(lookahead, TaintUtils.taintAt(stringToTokenize, position)));
                    return;
                }
                add(new Token(TaintUtils.toString(currentToken), position, quoteChar));
                currentToken = TaintUtils.newStringBuilder(TaintUtils
                        .toString(lookahead, TaintUtils.taintAt(stringToTokenize, position)));
                return;
            } else {
                if (currentToken.length() == 0) {
                    currentToken = new StringBuilder("" + lookahead);
                    return;
                }
                add(new Token(currentToken.toString(), position, quoteChar));
                currentToken = new StringBuilder("" + lookahead);
            }
        }

        /* UNUSED
        private void addTokenizedString_chartaint(char lookahead, char quoteChar) {
            if (tracked) {
                if (currentToken.length() == 0) {
                    lookahead = (CharMap.string_is_trusted_at(stringToTokenize,
                            position) ? CharMap.map_char(lookahead) : CharMap
                            .unmap_char(lookahead));
                    currentToken = new StringBuilder(
                            Character.toString(lookahead));
                    return;
                }
                add(new Token(currentToken.toString(), position, quoteChar));
                lookahead = (CharMap.string_is_trusted_at(stringToTokenize,
                        position) ? CharMap.map_char(lookahead) : CharMap
                        .unmap_char(lookahead));
                currentToken = new StringBuilder(Character.toString(lookahead));
                return;
            } else {
                if (currentToken.length() == 0) {
                    currentToken = new StringBuilder("" + lookahead);
                    return;
                }
                add(new Token(currentToken.toString(), position, quoteChar));
                currentToken = new StringBuilder("" + lookahead);
            }
        }
        */

        public Token getTokenClassAt(final int index) {
            final Token token = ((index < 0 || index > size() - 1) ? null : get(index));
            return token;
        }

        /**
         * Gets the index of the token containing the specified position in the 
         * untokenized string.
         * 
         * @param position - index within the currently tokenized string
         * @return index of the position
         */
        public int getTokenIndex(int position) {
            int size = size();
            for (int i = 0; i < size; i++) {
                if (get(i).contains(position))
                    return i;
            }
            return -1;
        }

        /**
         * Gets the token at the specified index, or null if the specified index
         * is invalid (i.e. out of bounds).  If the string to tokenize is tracked,
         * then the individual tokens will acquire the metadata of the string that
         * was tokenized.
         * 
         * @param index - token index
         * @return String - the token.
         */
        public String getTokenAt(int index) {
            if (index < 0 || index >= size())
                return null;
            return get(index).toString();
        }

        public String escapeQuotes() throws IOException {
            int totalOffset = 0;

            StringBuilder sb = TaintUtils.newStringBuilder(stringToTokenize);
            boolean modified = false;

            for (Token token : this) {
                char quoteBlockChar = token.getQuoteChar();
                if (quoteBlockChar == 0)
                    continue;

                char defaultEscape = defaultEscChars.get(quoteBlockChar);
                boolean replace = defaultEscape == 0;

                int start = token.getStart() + totalOffset;
                int end = token.getEnd() + totalOffset;

                int offset = sb.length();
                if (replace) {
                    modified = modified || replaceQuotes(sb, start, end, quoteBlockChar, token);
                } else {
                    modified = modified || escapeQuotes(sb, start, end, quoteBlockChar, defaultEscape);
                    totalOffset += (sb.length() - offset);
                }
            }
            return (modified) ? (TaintUtils.toString(sb)) : (stringToTokenize);
        }

        private boolean escapeQuotes(StringBuilder sb, int start, int end, char quoteChar, char defaultEscape)
                throws IOException {
            int idx = start;
            Set<Character> allEscapes = allEscChars.get(quoteChar);
            boolean selfEscaped = TaintUtils.charSetContains(allEscapes, quoteChar);
            //   boolean selfEscaped = allEscapes.contains(quoteChar);
            boolean modified = false;
            while (++idx < end) {
                char curr = sb.charAt(idx);
                if (TaintUtils.charMapContainsKey(defaultEscChars, curr)) { // it's a quote character...
                    //       if (defaultEscChars.containsKey(curr)) { // it's a quote character...
                    if (idx + 2 > end) {
                        TaintUtils.insert(sb, idx++, defaultEscape);
                        modified = true;
                        end++;
                        break;
                    }

                    char next = sb.charAt(idx + 1);
                    if (selfEscaped) {
                        if (TaintUtils.charEquals(next, curr)) {
                            //               if (next == curr) { 
                            // current char is escape char of next quote...
                            idx++;
                        } else if (TaintUtils.charEquals(quoteChar, curr)) {
                            //               } else if (quoteChar == curr){
                            TaintUtils.insert(sb, idx++, defaultEscape);
                            modified = true;
                            end++;
                        }
                    }
                } else if (TaintUtils.charSetContains(allEscapes, curr)) { // it's an escape character...
                    //       } else if (allEscapes.contains(curr)) { // it's an escape character...
                    if (idx + 2 > end) { // we are at the end, escape this escape char...
                        TaintUtils.insert(sb, idx++, curr);
                        modified = true;
                        end++;
                        break;
                    }

                    // check to see if the next character is a quote character,
                    // if not then escape the escape character...
                    char next = sb.charAt(idx + 1);
                    if (TaintUtils.charEquals(next, curr)) {
                        //      if (next == curr) {
                        idx++;
                    } else if (TaintUtils.charMapContainsKey(defaultEscChars, next)) {
                        //      } else if (defaultEscChars.containsKey(next)) {
                        if (TaintUtils.charEquals(next, quoteChar))
                            //          if (next == quoteChar)
                            TaintUtils.setCharAt(sb, idx++, defaultEscChars.get(next));
                        else {
                            TaintUtils.delete(sb, idx, idx + 1);
                            end--;
                        }
                        modified = true;
                    }
                }
            }
            return modified;
        }

        private boolean replaceQuotes(StringBuilder sb, int start, int end, char quoteChar, Token token) {
            int idx = start;
            String quoteToReplace = null;
            boolean shouldModify = false;
            boolean modified = false;
            for (Character qc : defaultEscChars.keySet()) {
                if (qc == null)
                    continue;
                char qChar = qc.charValue();
                if ((idx = sb.indexOf(qc.toString(), start + 1)) < 0 || idx >= end) {
                    if (quoteChar != qChar) {
                        quoteToReplace = qc.toString();
                    } else {
                        shouldModify = false;
                        break;
                    }
                } else if (quoteChar == qChar) {
                    shouldModify = true;
                }
            }

            if (!shouldModify)
                return modified;

            if (quoteToReplace == null) {
                throw new RuntimeException("the quoted token " + token + " contains all possible quote characters "
                        + "and therefore this quoted token block " + "cannot be replaced.");
            } else {
                TaintUtils.replace(sb, start, start + 1, quoteToReplace);
                TaintUtils.replace(sb, end, end + 1, quoteToReplace);
                modified = true;
            }
            return modified;
        }
    }

    private class CharMapEntry {
        private String escapeSequence;
        private int action;
        private Constructor<?> exception;
    }
}
