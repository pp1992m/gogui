//----------------------------------------------------------------------------
// SgfUtil.java
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SgfUtil
{
    /** Result of parseOvertime() */
    public static final class Overtime
    {
        public long m_byoyomi;

        public int m_byoyomiMoves;
    }

    public static Overtime parseOvertime(String value)
    {
        value = value.trim();
        Overtime result = null;

        /* Used by SgfWriter */
        result =
            parseOvertime(value,
                          "\\s*(\\d+)\\s*moves\\s*/\\s*(\\d+)\\s*sec\\s*",
                          true, 1000L);
        if (result != null)
            return result;

        /* Used by Smart Go */
        result =
            parseOvertime(value,
                          "\\s*(\\d+)\\s*moves\\s*/\\s*(\\d+)\\s*min\\s*",
                          true, 60000L);
        if (result != null)
            return result;

        /* Used by Kiseido Game Server, CGoban 2 */
        result =
            parseOvertime(value,
                          "\\s*(\\d+)x(\\d+)\\s*byo-yomi\\s*",
                          true, 1000L);
        if (result != null)
            return result;

        /* Used by ? */
        result =
            parseOvertime(value,
                          "\\s*(\\d+)x(\\d+)\\s*",
                          true, 1000L);
        if (result != null)
            return result;

        /* Used by Quarry, CGoban 2 */
        result =
            parseOvertime(value,
                          "\\s*(\\d+)/(\\d+)\\s*canadian\\s*",
                          true, 1000L);
        if (result != null)
            return result;

        return result;
    }

    /** Parse value of TM property.
        According to FF4, TM needs to be a real value, but older SGF versions
        allow a string with unspecified content. We try to parse a few known
        formats.
        @return The (pre-byoyomi-)time in milliseconds or -1, if the
        format was not recognized
    */
    public static long parseTime(String value)
    {
        value = value.trim();
        try
        {
            return (long)(Double.parseDouble(value) * 1000);
        }
        catch (NumberFormatException e1)
        {
        }
        try
        {
            Pattern pattern;
            Matcher matcher;

            // Pattern as written by CGoban 1.9.12
            pattern = Pattern.compile("(\\d{1,2}):(\\d{2})");
            matcher = pattern.matcher(value);
            if (matcher.matches())
            {
                assert matcher.groupCount() == 2;
                return (Integer.parseInt(matcher.group(1)) * 60000L
                        + Integer.parseInt(matcher.group(2)) * 1000L);
            }

            pattern = Pattern.compile("(\\d+):(\\d{2}):(\\d{2})");
            matcher = pattern.matcher(value);
            if (matcher.matches())
            {
                assert matcher.groupCount() == 3;
                return (Integer.parseInt(matcher.group(1)) * 3600000L
                        + Integer.parseInt(matcher.group(2)) * 60000L
                        + Integer.parseInt(matcher.group(3)) * 1000L);
            }

            // Formats found in some games of
            // http://www.cs.ualberta.ca/~mmueller/go/honinbo.html
            pattern = Pattern.compile("(\\d+)\\s*(?:h|hours|hours\\s+each)");
            matcher = pattern.matcher(value);
            if (matcher.matches())
            {
                assert matcher.groupCount() == 1;
                return Integer.parseInt(matcher.group(1)) * 3600000L;
            }
        }
        catch (NumberFormatException e2)
        {
            assert false; // patterns should match only valid integers
        }
        return -1;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private SgfUtil()
    {
    }

    private static Overtime parseOvertime(String value, String regex,
                                          boolean byoyomiMovesFirst,
                                          long timeUnitFactor)
    {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value);
        long byoyomi;
        int byoyomiMoves;
        if (matcher.matches())
        {
            assert matcher.groupCount() == 2;
            try
            {
                String group1;
                String group2;
                if (byoyomiMovesFirst)
                {
                    group1 = matcher.group(1);
                    group2 = matcher.group(2);
                }
                else
                {
                    group1 = matcher.group(2);
                    group2 = matcher.group(1);
                }
                Overtime overtime = new Overtime();
                overtime.m_byoyomiMoves = Integer.parseInt(group1);
                overtime.m_byoyomi =
                    (long)(Double.parseDouble(group2) * timeUnitFactor);
                return overtime;
            }
            catch (NumberFormatException e)
            {
                // should not happen if patterns match only integer
                assert false;
                return null;
            }
        }
        else
            return null;
    }
}
