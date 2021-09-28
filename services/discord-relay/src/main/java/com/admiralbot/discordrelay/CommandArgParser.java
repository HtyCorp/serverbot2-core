package com.admiralbot.discordrelay;

import com.admiralbot.sharedutil.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandArgParser {

    private final Logger logger = LoggerFactory.getLogger(CommandArgParser.class);

    private final static Pattern SQUOTE = Pattern.compile("\\s*'([^']*)'\\s*");
    private final static Pattern DQUOTE = Pattern.compile("\\s*\"([^\"]*)\"\\s*");
    private final static Pattern WORD = Pattern.compile("\\s*([^\\s'\"]+)\\s*");
    private final static List<Pattern> ALL_PATTERNS = List.of(SQUOTE, DQUOTE, WORD);

    public List<String> parseArgs(String input) {
        List<String> args = new ArrayList<>();
        String currentInput = input;
        while (!currentInput.isBlank()) {
            Pair<String,String> parseOne = tryAllPatterns(currentInput);
            if (parseOne == null) {
                logger.warn("Got a null parse result on a non-blank string (remaining input:'" + currentInput + "')");
                break;
            }
            args.add(parseOne.a());
            currentInput = parseOne.b();
        }
        return args;
    }

    public Pair<String, String> tryAllPatterns(String input) {
        for (Pattern p: ALL_PATTERNS) {
            Pair<String,String> result = tryMatch(p, input);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public Pair<String, String> tryMatch(Pattern pattern, String input) {
        Matcher m = pattern.matcher(input);
        if (m.lookingAt()) {
            String content = m.group(1);
            String rest = input.substring(m.group(0).length());
            return new Pair<>(content, rest);
        }
        return null;
    }

}
