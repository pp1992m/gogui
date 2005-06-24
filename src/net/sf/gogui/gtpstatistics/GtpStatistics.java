//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import java.text.DateFormat;
import java.text.DecimalFormat;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.Gtp;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.Statistics;
import net.sf.gogui.utils.Table;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

public class GtpStatistics
{
    public GtpStatistics(String program, Vector sgfFiles, Vector commands,
                         boolean verbose)
        throws Exception
    {
        m_result = false;
        m_commands = commands;
        Vector columnHeaders = new Vector();
        columnHeaders.add("File");
        columnHeaders.add("Move");
        if (m_runRegGenMove)
            columnHeaders.add("reg_genmove");
        if (commands != null)
            for (int i = 0; i < commands.size(); ++i)
                columnHeaders.add(getCommand(i));
        m_table = new Table(columnHeaders);
        m_gtp = new Gtp(program, verbose, null);
        m_table.setProperty("Program", program);
        try
        {
            m_table.setProperty("Name", m_gtp.sendCommand("name"));
        }
        catch (GtpError e)
        {
            m_table.setProperty("Name", "");
            if (m_gtp.isProgramDead())
                throw e;
        }
        try
        {
            m_table.setProperty("Version", m_gtp.sendCommand("version"));
        }
        catch (GtpError e)
        {
            m_table.setProperty("Version", "");
        }
        String host;
        try
        {
            host = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            host = "?";
        }
        m_table.setProperty("Host", host);
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.FULL,
                                                           DateFormat.FULL);
        Date date = Calendar.getInstance().getTime();
        m_table.setProperty("Date", format.format(date));
        for (int i = 0; i < sgfFiles.size(); ++i)
            handleFile((String)sgfFiles.get(i));
        m_table.setProperty("Games", Integer.toString(m_numberGames));
        FileWriter writer = new FileWriter("gtpstatistics.dat");
        m_table.save(writer);
        writer.close();
    }

    public boolean getResult()
    {
        return m_result;
    }

    private boolean m_result;

    private boolean m_runRegGenMove = true;

    private int m_numberGames;

    private Gtp m_gtp;

    private Table m_table;

    private Vector m_commands;

    private String getCommand(int index)
    {
        return (String)m_commands.get(index);
    }

    private void handleFile(String name)
        throws ErrorMessage, FileNotFoundException, GtpError,
               SgfReader.SgfError
    {
        System.err.println(name);
        InputStream in = new FileInputStream(new File(name));
        SgfReader reader = new SgfReader(in, name, null, 0);
        ++m_numberGames;
        GameTree tree = reader.getGameTree();
        GameInformation info = tree.getGameInformation();
        int size = info.m_boardSize;
        m_gtp.sendCommandBoardsize(size);
        m_gtp.sendCommandClearBoard(size);
        Node root = tree.getRoot();
        if (root.getMove() != null)
            throw new ErrorMessage(name + " has move in root node");
        int number = 0;
        for (Node node = root; node != null; node = node.getChild())
        {
            if (node.getNumberAddWhite() + node.getNumberAddBlack() > 0)
                throw new ErrorMessage(name + " contains setup stones");
            Move move = node.getMove();
            if (move != null)
            {
                ++number;
                handlePosition(name, move, number);
                m_gtp.sendCommandPlay(move);
            }
        }
    }

    private void handlePosition(String name, Move move, int number)
        throws GtpError
    {
        m_table.startRow();
        m_table.set("File", name);
        m_table.set("Move", number);
        if (m_runRegGenMove && move != null)
        {
            boolean result = runRegGenMove(move);
            m_table.set("reg_genmove", result ? "1" : "0");
        }
        if (m_commands == null)
            return;
        for (int i = 0; i < m_commands.size(); ++i)
        {
            String command = getCommand(i);
            String result = m_gtp.sendCommand(command);
            m_table.set(command, result);
        }
    }

    private boolean runRegGenMove(Move move) throws GtpError
    {
        String response = m_gtp.sendCommand("reg_genmove " + move.getColor());
        response = response.trim().toUpperCase();
        return (response.equals(GoPoint.toString(move.getPoint())));
    }
}
    
//----------------------------------------------------------------------------
