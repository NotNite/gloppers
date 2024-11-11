package com.notnite.gloppers;

import java.util.BitSet;

public class GlobUtil {

    public static boolean matchGlobSequence(String name, String globs) {
        int nextSplit, lastSplit = 0;
        while ((nextSplit = globs.indexOf(',', lastSplit)) > 0) {
            if (matchGlob(name, globs.substring(lastSplit, nextSplit)))
                return true;
            lastSplit = nextSplit + 1;
        }
        return matchGlob(name, globs.substring(lastSplit));
    }

    /**
     * Match a string against a glob.
     */
    public static boolean matchGlob(String name, String glob) {
        // While iterating over the name, every prefix of the glob that is matched has a bit set in this bitset
        // For example: if we have iterated over the string "abb" and our glob is "*bb" the bits 1 and 2 would be set,
        // since our current string matches both "*b" and "*bb". After we have iterated over the entirety of the string
        // We can simply check if the highest bit is set, in which case our entire string matched the entire glob
        // (since the entire glob is the longest prefix of the glob).
        var bitSet = new BitSet(glob.length() + 1);
        var swapBitSet = new BitSet(glob.length() + 1);

        // Set the first prefix of the glob (the empty prefix) as matched
        bitSet.set(0);

        // Iterate over all chars
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            int nextSetBit = -1;

            // Iterate over all existing matches and try to advance them by one glob char
            while (true) {
                nextSetBit = bitSet.nextSetBit(nextSetBit + 1);
                if (nextSetBit == -1 || nextSetBit == glob.length()) break;
                char globChar = glob.charAt(nextSetBit);
                switch (globChar) {
                    case '?': // In case of a question mark (any single character matches)
                        // Set the next bit as matched
                        swapBitSet.set(nextSetBit + 1);
                        break;
                    case '*': // In case of a question mark (any number of characters matches)
                        // Set the current bit as matched (since we allow this character to be matched multiple times)
                        swapBitSet.set(nextSetBit);
                        // Set the next bit as matched
                        swapBitSet.set(nextSetBit + 1);
                        break;
                    default: // No special character
                        if (c == globChar) { // If the glob char is correct on its own
                            swapBitSet.set(nextSetBit + 1);
                        }
                        break;
                }
            }
            // If there are no currently matched glob prefixes (including the empty one), there is no match.
            if (swapBitSet.isEmpty()) return false;
            // Swap the swap bit set for the main one and clear the new swap bit set so it can be filled again.
            var temp = swapBitSet;
            swapBitSet = bitSet;
            bitSet = temp;
            swapBitSet.clear();
        }

        // Since * globs can match 0 characters, we need to loop over the remaining bitset with pseudo "empty"
        // characters, in order to allow the glob to end with a *
        for (int i = glob.length() - 1; i >= 0; i--) {
            char globChar = glob.charAt(i);
            if (globChar != '*') break;
            if (bitSet.get(i)) return true;
        }
        return bitSet.get(glob.length());
    }

}
