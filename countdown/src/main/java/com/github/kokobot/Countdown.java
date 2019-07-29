package com.github.kokobot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Countdown {
    public static void main(String[] args) {
        String token = "NDYzOTcyNTk1ODIyMzYyNjI0.XTt3kg.BiO_8LFejTGHgrAskyjxsOf_w0I";
        DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();

        api.addMessageCreateListener(Countdown::onMessageCreate);
        // Print the invite url of your bot
        System.out.println("You can invite the bot by using the following url: " + api.createBotInvite());
    }

    private static void onMessageCreate(MessageCreateEvent event) {
        String message = event.getMessage().getContent();

        if (message.startsWith("!cd") || message.startsWith("!CD")) {
            String prompt = message.substring(3);
            if (prompt.startsWith(" n") || prompt.startsWith(" m")) {
                prompt = prompt.substring(2);
                if (prompt.isEmpty()) {
                    printN(event);
                } else if (prompt.equals(" new")) {
                    printRoundLeaderN(event);
                    processScoresN();
                    setNewN();
                    printN(event);
                } else if (prompt.equals(" scores")) {
                    printScoresN(event);
                } else {
                    checkSolutionN(prompt, event);
                }
            } else if (prompt.startsWith(" l") || prompt.startsWith(" w")) {
                prompt = prompt.substring(2);
                if (prompt.isEmpty()) {
                    printL(event);
                } else if (prompt.equals(" 0")) {
                    printRoundLeaderL(event);
                    processScoresL();
                    setNewL();
                    printL(event);
                } else if (prompt.equals(" 1")) {
                    printScoresL(event);
                } else {
                    checkSolutionL(prompt, event);
                }
            } else if (prompt.startsWith(" c")) {
                prompt = prompt.substring(2);
                if (prompt.isEmpty()) {
                    printC(event);
                } else {
                    checkSolutionC(prompt, event);
                }
            } else if (prompt.isEmpty()) {
                printIntro(event);
            } else if (isExpression(prompt)) {
            checkSolutionN(prompt, event);
            }
        }
    }

    private static Scanner scanFile(String fileName) {
        Scanner theFile = null;
        try {
            theFile = new Scanner( new FileInputStream(fileName));
        } catch (FileNotFoundException e) {
            System.out.println("Broken 1.");
        }
        return theFile;
    }

    private static PrintWriter writeFile(String fileName) {
        PrintWriter theFile = null;
        try {
            theFile = new PrintWriter( new FileOutputStream(fileName));
        } catch (FileNotFoundException e) {
            System.out.println("Broken 2.");
        }
        return theFile;
    }

    private static int[] scanN() {
        Scanner mFile = scanFile("n.txt");
        int[] num = new int[8];

        for (int i=0; i<6; i++) {
            num[i] = mFile.nextInt();
        }

        mFile.nextLine();
        num[6] = mFile.nextInt();
        num[7] = mFile.nextInt();
        mFile.close();
        return num;
    }

    private static void printN(MessageCreateEvent event) {
        int [] num = scanN();
        String message = "Numbers:";

        EmbedBuilder e = new EmbedBuilder().setTitle("NUMBERS ROUND " + num[7]);

        for (int i=0; i<6; i++) {
            message += "\t\t" + num[i];
        }

        message += "\nTarget:\t\t" + num[6];
        event.getChannel().sendMessage(e);
        event.getChannel().sendMessage(message);

    }

    private static void checkSolutionN(String expression, MessageCreateEvent event) {
        int[] nUsed = getNumbersUsed(expression);
        int[] nM = scanN();
        int target = nM[6];
        nM = Arrays.copyOfRange(nM, 0, 6);

        if (!isArrayASubset(nM, nUsed)) {
            printErrorExpressionN(event);
            return;
        }

        int ans = solveExpression(expression);
        EmbedBuilder e = new EmbedBuilder();
        String message;

        if (ans == Integer.MIN_VALUE) {
            printErrorExpressionN(event);
            return;
        }

        String author = event.getMessage().getAuthor().getDisplayName();
        int diff = Math.abs(ans - target);

        message = "\n" + expression + " = " + ans;
        message += "\nYou are " + diff + " away.";
        e.setTitle(author + "'s SOLUTION").setDescription(message);
        event.getChannel().sendMessage(e);

        addScoreN(author, diff);
        if (diff == 0) {
            EmbedBuilder e2 = new EmbedBuilder().setDescription(author + " gains 10 points.");
            event.getChannel().sendMessage(e2);
            setNewN();
        }
        printCurrentLeaderN(event);
        printN(event);

    }

    private static void printErrorExpressionN(MessageCreateEvent event) {
        EmbedBuilder e = new EmbedBuilder();
        String message = "Incorrect numbers or expression.";
        e.setTitle("ERROR").setDescription(message);
        event.getChannel().sendMessage(e);
        printN(event);
    }

    private static void printErrorExpressionL(MessageCreateEvent event, int n) {
        EmbedBuilder e = new EmbedBuilder();
        String message = "Incorrect letters or word.";
        if (n == -1) {
            message = "Incorrect letters.";
        } else if (n == -2) {
            message = "Not a word.";
        }
        e.setTitle("ERROR").setDescription(message);
        event.getChannel().sendMessage(e);
        printL(event);
    }

    private static int[] getNumbersUsed(String expression) {
        String[] S = expression.split("\\D+");
        int n = 0;
        int[] intArray = new int[S.length];
        for (String s:S) {
            if (s == null || s.equals("")) {
                continue;
            }
            try {
                intArray[n] = Integer.parseInt(s);
                n++;
            } catch (NumberFormatException nfe) {
                System.out.println("Broken 3: " + s);
            }
        }

        return Arrays.copyOfRange(intArray, 0, n);
    }

    private static boolean isArrayASubset(int[] main, int[] sub) {
        int count = 0;
        for (int i = 0; i < sub.length; i++) {
            for (int j = 0; j < main.length; j++) {
                if (main[j] == sub[i]) {
                    main[j] = -1;
                    count++;
                    break;
                }
            }
        }
        return (count == sub.length);
    }

    private static boolean isArrayASubset(char[] main, char[] sub) {
        int count = 0;
        for (int i = 0; i < sub.length; i++) {
            for (int j = 0; j < main.length; j++) {
                if (main[j] == sub[i]) {
                    main[j] = ' ';
                    count++;
                    break;
                }
            }
        }
        return (count == sub.length);
    }

    private static int solveExpression(String expression) {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        int ans;
        try {
            ans = (int) engine.eval(expression);
            return ans;
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    private static void addScoreN(String author, int diff) {
        if (diff > 10) {
            return;
        }

        Scanner nFile = scanFile("scoresN.txt");
        int n = nFile.nextInt(), pn = -1, currentScore;
        int[] currentScores, totalScores = new int[n];
        String[] players = new String[n];

        nFile.nextLine();

        for (int i=0; i<n; i++) {
            players[i] = nFile.nextLine();
            currentScore = nFile.nextInt();

            if (currentScore <= diff) {
                nFile.close();
                return;
            }

            totalScores[i] = nFile.nextInt();
            nFile.nextLine();

            if (players[i].equals(author)) {
                pn = i;
            }
        }
        nFile.close();

        if (pn == -1) {
            players = Arrays.copyOf(players, n+1);
            players[n] = author;
            n++;
            pn = n-1;
            totalScores = Arrays.copyOf(totalScores, n);
        }

        currentScores = initialiseCurrentScoresN(n);
        if (diff == 0) {
            totalScores[pn] += 10;
        } else {
            currentScores[pn] = diff;
        }

        writeScoresN(players, currentScores, totalScores);

    }

    private static int[] initialiseCurrentScoresN(int n) {
        int[] A = new int[n];
        for (int i=0; i<n; i++) {
            A[i] = 11;
        }
        return A;
    }

    private static void writeScoresN(String[] players, int[] currentScores, int[] totalScores) {
        PrintWriter nFile = writeFile("scoresN.txt");
        int n = players.length;
        nFile.println(n);
        for (int i=0; i<n; i++) {
            nFile.println(players[i]);
            nFile.println(String.format("%d %d", currentScores[i], totalScores[i]));
        }
        nFile.close();
    }

    private static void setNewN() {
        int[] num = scanN(), newN = getNewN();

        for (int i=0; i<7; i++) {
            num[i] = newN[i];
        }
        num[7]++;
        writeN(num);
    }

    private static int[] getNewN() {
        int[] num = new int[7], large = new int[4], small = new int[20];

        for (int i = 0; i < 4; i++) {
            large[i] = 25 + 25 * i;
        }

        for (int i = 0; i < 20; i++) {
            small[i] = (i / 2) + 1;
        }

        int n = getNumLarge();

        int[] nSmall, nLarge;

        nLarge = new Random().ints(0, 4).distinct().limit(n).toArray();
        nSmall = new Random().ints(0, 20).distinct().limit(6-n).toArray();

        for (int i = 0; i < 6; i++) {
            if (i < n) {
                num[i] = large[nLarge[i]];
            } else {
                num[i] = small[nSmall[i - n]];
            }
        }

        num[6] = new Random().nextInt(898) + 101;

        return num;
    }

    private static int getNumLarge() {
        int count = 0;
        double r;
        for (int i=0; i<6; i++) {
            r = Math.random();
            if (r < 2.0/6) {
                count++;
            }
        }
        if (count > 4) {
            return getNumLarge();
        }
        return count;
    }

    private static void writeN(int[] n) {
        PrintWriter nFile = writeFile("n.txt");
        String s = Integer.toString(n[0]);
        for (int i=1; i<6; i++) {
            s += " " + n[i];
        }
        nFile.println(s);
        s = String.format("%d %d", n[6], n[7]);
        nFile.println(s);
        nFile.close();
    }

    private static void processScoresN() {
        Scanner nFileIn = scanFile("scoresN.txt");
        PrintWriter nFileOut = writeFile("tmp.txt");
        int n = nFileIn.nextInt(), totalScore;
        String player;

        nFileOut.println(n);
        nFileIn.nextLine();

        for (int i=0; i<n; i++) {
            player = nFileIn.nextLine();
            totalScore = nFileIn.nextInt();
            totalScore = getScoreN(totalScore);
            totalScore += nFileIn.nextInt();
            nFileIn.nextLine();

            nFileOut.println(player);
            nFileOut.println(String.format("11 %d", totalScore));
        }

        nFileIn.close();
        nFileOut.close();
        renameFile("tmp.txt", "scoresN.txt");
    }

    private static int getScoreN(int n) {
        if (n > 10) {
            return 0;
        } else if (n > 5) {
            return 5;
        } else {
            return 7;
        } // case where difference is 0 is dealt already
    }

    private static void renameFile(String oldName, String newName) {
        File tmp = new File(oldName);
        File game = new File(newName);
        tmp.renameTo(game);
    }

    private static void printScoresN(MessageCreateEvent event) {
        Scanner nFile = scanFile("scoresN.txt");
        int n = nFile.nextInt(), tmpS;
        int[] totalScores = new int[n];
        String [] players = new String[n];
        String tmpP, message;

        nFile.nextLine();
        for (int i=0; i<n; i++) {
            players[i] = nFile.nextLine();
            nFile.nextInt();
            totalScores[i] = nFile.nextInt();
            nFile.nextLine();

            for (int j=i; j>0; j--) {
                if (totalScores[j] > totalScores[j-1]) {
                    tmpP = players[j-1];
                    tmpS = totalScores[j-1];
                    players[j-1] = players[j];
                    totalScores[j-1] = totalScores[j];
                    players[j] = tmpP;
                    totalScores[j] = tmpS;
                } else {
                    break;
                }
            }
        }
        nFile.close();

        int rank = 1;
        if (n > 1 && totalScores[0] == totalScores[1]) {
            message = rank + "= " + players[0] + ":\t" + totalScores[0];
        } else {
            message = rank + ".  " + players[0] + ":\t" + totalScores[0];
            rank++;
        }

        for (int i=1; i<n; i++) {
            if (totalScores[i] == totalScores[i-1]) {
                message += "\n" + rank + "= " + players[i] + ":\t" + totalScores[i];
            } else {
                message += "\n" + rank + ". " + players[i] + ":\t" + totalScores[i];
                rank++;
            }
        }

        EmbedBuilder e = new EmbedBuilder().setTitle("NUMBERS LEADERBOARD");
        event.getChannel().sendMessage(e);
        event.getChannel().sendMessage(message);
    }

    private static void printIntro(MessageCreateEvent event) {
        EmbedBuilder e = new EmbedBuilder().setTitle("COUNTDOWN BOT").setDescription("Type !cd help");
        event.getChannel().sendMessage(e);
        printN(event);
        printL(event);
        printC(event);
    }

    private static Boolean isExpression(String prompt) {
        char c0 = prompt.charAt(0), c1 = prompt.charAt(1);
        return (c0 == ' ') && (c1 == '(' || c1 == '-' || Character.isDigit(c1));
    }

    private static char[] scanL() {
        Scanner Lfile = scanFile("l.txt");
        String letters;
        char[] c = new char[9];

        letters = Lfile.nextLine();

        for (int i=0; i<9; i++) {
            c[i] = letters.charAt(i);
        }
        Lfile.close();
        return c;
    }

    private static int scanNumL() {
        Scanner Lfile = scanFile("l.txt");
        Lfile.nextLine();
        int n = Lfile.nextInt();
        Lfile.close();
        return n;
    }

    private static void printL(MessageCreateEvent event) {
        char[] let = scanL();
        int n = scanNumL();
        String message = "Letters:";

        EmbedBuilder e = new EmbedBuilder().setTitle("LETTERS ROUND " + n);
        let = getRandArrangementL(let);

        for (int i=0; i<9; i++) {
            message += "\t\t" + let[i];
        }

        event.getChannel().sendMessage(e);
        event.getChannel().sendMessage(message);
    }

    private static boolean isWord(String word) {
        word = word.toLowerCase();
        try {
            BufferedReader in = new BufferedReader(new FileReader(
                    "/usr/share/dict/words"));
            String str;
            while ((str = in.readLine()) != null) {
                if (str.equals(word)) {
                    return true;
                }
            }
            in.close();
        } catch (IOException e) {
            System.out.println("Broken 4");
        }

        return false;
    }

    private static char[] getChar(String word) {
        int n = word.length();
        char[] c = new char[n];
        for (int i=0; i<n; i++) {
            c[i] = word.charAt(i);
        }
        return c;
    }

    private static Boolean isLetters(char[] w) {
        if (w[0] != ' ') {
            return false;
        }

        for (char c:Arrays.copyOfRange(w, 1, w.length)) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    private static void checkSolutionL(String word, MessageCreateEvent event) {
        char[] c = getChar(word);
        char[] w = Arrays.copyOfRange(c, 1, c.length);

        if (!isLetters(c)) {
            printErrorExpressionL(event, -1);
            return;
        }

        String W = new String(w).toUpperCase();
        char[] letters = scanL();

        if (!isArrayASubset(letters, getChar(W))) {
            printErrorExpressionL(event, -1);
            return;
        }

        if (!isWord(W)) {
            printErrorExpressionL(event, -2);
            return;
        }

        EmbedBuilder e = new EmbedBuilder();
        String message;

        String author = event.getMessage().getAuthor().getDisplayName();
        int len = W.length();

        message = W + " is a " + len + " letter word.";
        e.setTitle(author + "'s WORD").setDescription(message);
        event.getChannel().sendMessage(e);

        addScoreL(author, len);
        if (len == 9) {
            EmbedBuilder e2 = new EmbedBuilder().setDescription(author + " gains 18 points.");
            event.getChannel().sendMessage(e2);
            setNewL();
        }
        printCurrentLeaderL(event);
        printL(event);

    }

    private static int getNumVowels() {
        int count = 0;
        double r;
        for (int i=0; i<9; i++) {
            r = Math.random();
            if (r < 4.0/9) {
                count++;
            }
        }
        if (count < 3 || count > 5) {
            return getNumLarge();
        }
        return count;
    }

    private static char[] getVowels(int n) {
        char[] V = new char[67];
        char[] vowel = {'A', 'E', 'I', 'O', 'U'};
        int[] vowelDist = {15, 21, 13, 13, 5};
        int k=0;
        for (int i=0; i<5; i++) {
            for (int j=0; j<vowelDist[i]; j++) {
                V[k] = vowel[i];
                k++;
            }
        }

        int[] nV = new Random().ints(0, 67).distinct().limit(n).toArray();

        char[] selected = new char[n];
        for (int i=0; i<n; i++) {
            selected[i] = V[nV[i]];
        }
        return selected;
    }

    private static char[] getConsonants(int n) {
        char[] C = new char[74];
        char[] cons = {'B', 'C', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X',
                'Y', 'Z'};
        int[] consDist = {2,3,6,2,3,2,1,1,5,4,8,4,1,9,9,9,1,1,1,1,1};
        int k=0;
        for (int i=0; i<21; i++) {
            for (int j=0; j<consDist[i]; j++) {
                C[k] = cons[i];
                k++;
            }
        }

        int[] nC = new Random().ints(0, 74).distinct().limit(n).toArray();

        char[] selected = new char[n];
        for (int i=0; i<n; i++) {
            selected[i] = C[nC[i]];
        }
        return selected;
    }

    private static char[] getNewL() {
        int n = getNumVowels();
        char[] L = new char[9], v = getVowels(n), c = getConsonants(9-n);
        System.arraycopy(v,0,L,0,n);
        System.arraycopy(c,0,L,n,9-n);

        return getRandArrangementL(L);
    }

    private static void setNewL() {
        int n = scanNumL();
        char[] c = getNewL();

        writeL(c, n+1);
    }

    private static void writeL(char[] c, int n) {
        PrintWriter Lfile = writeFile("l.txt");
        for (int i=0; i<9; i++) {
            Lfile.print(c[i]);
        }
        Lfile.println();
        Lfile.println(n);
        Lfile.close();
    }

    private static void processScoresL() {
        Scanner lFileIn = scanFile("scoresL.txt");
        PrintWriter lFileOut = writeFile("tmp.txt");
        int n = lFileIn.nextInt(), totalScore;
        String player;

        lFileOut.println(n);
        lFileIn.nextLine();

        for (int i=0; i<n; i++) {
            player = lFileIn.nextLine();
            totalScore = lFileIn.nextInt();
            totalScore += lFileIn.nextInt();
            lFileIn.nextLine();

            lFileOut.println(player);
            lFileOut.println(String.format("0 %d", totalScore));
        }

        lFileIn.close();
        lFileOut.close();
        renameFile("tmp.txt", "scoresL.txt");
    }

    private static void addScoreL(String author, int w) {
        Scanner lFile = scanFile("scoresL.txt");
        int n = lFile.nextInt(), pn = -1, currentScore;
        int[] currentScores, totalScores = new int[n];
        String[] players = new String[n];

        lFile.nextLine();

        for (int i=0; i<n; i++) {
            players[i] = lFile.nextLine();
            currentScore = lFile.nextInt();

            if (currentScore >= w) {
                lFile.close();
                return;
            }

            totalScores[i] = lFile.nextInt();
            lFile.nextLine();

            if (players[i].equals(author)) {
                pn = i;
            }
        }
        lFile.close();

        if (pn == -1) {
            players = Arrays.copyOf(players, n+1);
            players[n] = author;
            n++;
            pn = n-1;
            totalScores = Arrays.copyOf(totalScores, n);
        }

        currentScores = new int[n];
        if (w == 9) {
            totalScores[pn] += 18;
        } else {
            currentScores[pn] = w;
        }

        writeScoresL(players, currentScores, totalScores);

    }

    private static void writeScoresL(String[] players, int[] currentScores, int[] totalScores) {
        PrintWriter lFile = writeFile("scoresL.txt");
        int n = players.length;
        lFile.println(n);
        for (int i=0; i<n; i++) {
            lFile.println(players[i]);
            lFile.println(String.format("%d %d", currentScores[i], totalScores[i]));
        }
        lFile.close();
    }

    private static void printScoresL(MessageCreateEvent event) {
        Scanner lFile = scanFile("scoresL.txt");
        int n = lFile.nextInt(), tmpS;
        int[] totalScores = new int[n];
        String [] players = new String[n];
        String tmpP, message;

        lFile.nextLine();
        for (int i=0; i<n; i++) {
            players[i] = lFile.nextLine();
            lFile.nextInt();
            totalScores[i] = lFile.nextInt();
            lFile.nextLine();

            for (int j=i; j>0; j--) {
                if (totalScores[j] > totalScores[j-1]) {
                    tmpP = players[j-1];
                    tmpS = totalScores[j-1];
                    players[j-1] = players[j];
                    totalScores[j-1] = totalScores[j];
                    players[j] = tmpP;
                    totalScores[j] = tmpS;
                } else {
                    break;
                }
            }
        }
        lFile.close();

        int rank = 1;
        if (n > 1 && totalScores[0] == totalScores[1]) {
            message = rank + "= " + players[0] + ":\t" + totalScores[0];
        } else {
            message = rank + ".  " + players[0] + ":\t" + totalScores[0];
            rank++;
        }

        for (int i=1; i<n; i++) {
            if (totalScores[i] == totalScores[i-1]) {
                message += "\n" + rank + "= " + players[i] + ":\t" + totalScores[i];
            } else {
                message += "\n" + rank + ". " + players[i] + ":\t" + totalScores[i];
                rank++;
            }
        }

        EmbedBuilder e = new EmbedBuilder().setTitle("LETTERS LEADERBOARD");
        event.getChannel().sendMessage(e);
        event.getChannel().sendMessage(message);
    }

    private static char[] getRandArrangementL(char[] L) {
        int[] nL = new Random().ints(0, 9).distinct().limit(9).toArray();
        char[] L2 = new char[9];

        for (int i=0; i<9; i++) {
            L2[i] = L[nL[i]];
        }
        return L2;
    }

    private static String[] getCurrentLeader(char c) {
        String[] leader = new String[2];
        if (c == 'l') {
            Scanner lFile = scanFile("scoresL.txt");
            int n = lFile.nextInt(), currentScore;
            String player;

            lFile.nextLine();

            for (int i=0; i<n; i++) {
                player = lFile.nextLine();
                currentScore = lFile.nextInt();

                if (currentScore > 0) {
                    lFile.close();
                    leader[0] = player;
                    leader[1] = String.valueOf(currentScore);
                    return leader;
                }
                lFile.nextLine();

            }
            lFile.close();
        } else if (c == 'n') {
            Scanner nFile = scanFile("scoresN.txt");
            int n = nFile.nextInt(), currentScore;
            String player;

            nFile.nextLine();

            for (int i=0; i<n; i++) {
                player = nFile.nextLine();
                currentScore = nFile.nextInt();

                if (currentScore < 11) {
                    nFile.close();
                    leader[0] = player;
                    leader[1] = String.valueOf(currentScore);
                    return leader;
                }
                nFile.nextLine();

            }
            nFile.close();
        }
        return leader;
    }

    private static void printCurrentLeaderN(MessageCreateEvent event) {
        String[] leader = getCurrentLeader('n');
        if (leader[0].isEmpty()) {
            return;
        }
        String message = leader[0] + " is the current leader with " + leader[1] + " away.";
        EmbedBuilder e = new EmbedBuilder().setDescription(message);
        event.getChannel().sendMessage(e);
    }

    private static void printCurrentLeaderL(MessageCreateEvent event) {
        String[] leader = getCurrentLeader('l');
        if (leader[0].isEmpty()) {
            return;
        }
        String message = leader[0] + " is the current leader with a " + leader[1] + " letter word.";
        EmbedBuilder e = new EmbedBuilder().setDescription(message);
        event.getChannel().sendMessage(e);
    }

    private static void printRoundLeaderN(MessageCreateEvent event) {
        String[] leader = getCurrentLeader('n');
        if (leader[0].isEmpty()) {
            return;
        }
        String message = leader[0] + " gains " + getScoreN(Integer.valueOf(leader[1])) + " points.";
        EmbedBuilder e = new EmbedBuilder().setDescription(message);
        event.getChannel().sendMessage(e);
    }

    private static void printRoundLeaderL(MessageCreateEvent event) {
        String[] leader = getCurrentLeader('l');
        if (leader[0].isEmpty()) {
            return;
        }
        String message = leader[0] + " gains " + Integer.valueOf(leader[1]) + " points.";
        EmbedBuilder e = new EmbedBuilder().setDescription(message);
        event.getChannel().sendMessage(e);
    }

    private static void printC(MessageCreateEvent event) {
        char[] let = scanC();
        int n = scanNumC();
        String message = "Letters:";

        EmbedBuilder e = new EmbedBuilder().setTitle("CONUNDRUM ROUND " + n);
        let = getRandArrangementL(let);

        for (int i=0; i<9; i++) {
            message += "\t\t" + let[i];
        }

        event.getChannel().sendMessage(e);
        event.getChannel().sendMessage(message);
    }

    private static char[] scanC() {
        Scanner cFile = scanFile("c.txt");
        String letters;
        char[] c = new char[9];

        letters = cFile.nextLine();

        for (int i=0; i<9; i++) {
            c[i] = letters.charAt(i);
        }
        cFile.close();
        return c;
    }

    private static int scanNumC() {
        Scanner cFile = scanFile("c.txt");
        cFile.nextLine();
        int n = cFile.nextInt();
        cFile.close();
        return n;
    }

    private static void checkSolutionC(String word, MessageCreateEvent event) {
        char[] c = getChar(word);
        char[] w = Arrays.copyOfRange(c, 1, c.length);

        if (!isLetters(c)) {
            printErrorExpressionC(event, -1);
            return;
        }

        String W = new String(w).toUpperCase();

        if (!W.equals(new String(scanC()))) {
            printErrorExpressionC(event, -2);
            return;
        }

        EmbedBuilder e = new EmbedBuilder();
        String message;

        String author = event.getMessage().getAuthor().getDisplayName();

        message = W + " is the conundrum.\n" + author + " gains 10 points.";
        e.setTitle(author + "'s WORD").setDescription(message);
        event.getChannel().sendMessage(e);

        addScoreC(author);
        setNewC();
        printC(event);

    }

    private static void printErrorExpressionC(MessageCreateEvent event, int n) {
        EmbedBuilder e = new EmbedBuilder();
        String message = "Incorrect letters or word.";
        if (n == -1) {
            message = "Incorrect letters.";
        } else if (n == -2) {
            message = "Incorrect word.";
        }
        e.setTitle("ERROR").setDescription(message);
        event.getChannel().sendMessage(e);
        printC(event);
    }

    private static void addScoreC(String author) {
        Scanner cFileIn = scanFile("scoresC.txt");
        PrintWriter cFileOut = writeFile("tmp.txt");
        int n = cFileIn.nextInt(), isFound=0;
        int[] scores = new int[n];
        String[] players = new String[n];


        cFileIn.nextLine();

        for (int i=0; i<n; i++) {
            players[i] = cFileIn.nextLine();
            scores[i] = cFileIn.nextInt();

            cFileIn.nextLine();

            if (players[i].equals(author)) {
                scores[i] += 10;
                isFound = 1;
            }
        }
        cFileIn.close();

        if (isFound == 0) {
            n++;
        }

        cFileOut.println(n);
        for (int i=0; i<n; i++) {
            cFileOut.println(players[i]);
            cFileOut.println(scores[i]);
        }

        if (isFound == 0) {
            cFileOut.println(author);
            cFileOut.println(10);
        }

        cFileOut.close();
        renameFile("tmp.txt", "scoresC.txt");
    }

    private static void setNewC() {
        Scanner cFileIn = scanFile("conundrum.txt");
        String w = "";
        int n = cFileIn.nextInt();
        n = new Random().nextInt(n)+1;
        for (int i=0; i<n; i++) {
            w = cFileIn.nextLine();
        }
        cFileIn.close();
        n = scanNumC();
        n++;
        PrintWriter cFileOut = writeFile("c.txt");
        cFileOut.println(w);
        cFileOut.println(n);
        cFileOut.close();
    }

}