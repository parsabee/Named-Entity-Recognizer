import java.io.*;
import java.util.*;

import javafx.util.Pair;

import javax.swing.event.ListDataEvent;

public class FeatureBuilder {
    public class ListDataLoader {

        HashSet<String> set;

        public ListDataLoader(String fileName) {
            set = new HashSet<>();

            try {
                File myObj = new File(fileName);
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    String[] items = data.split(" ");
                    if (items.length > 0)
                        set.add(items[0].toUpperCase());
                }
                myReader.close();
            } catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }

        public boolean contains(String element) {
            return set.contains(element);
        }
    }


    File file;
    String outFileName;

    HashMap<String, ListDataLoader> dataLoaders;

    public FeatureBuilder(String inputFile, String outputFile) {
        outFileName = outputFile;
        file = new File(inputFile);
        dataLoaders = new HashMap<>();
    }

    public void addFeatureList(String fileName, String featureName) {
        ListDataLoader dataLoader = new ListDataLoader(fileName);
        dataLoaders.put(featureName, dataLoader);
    }

    /* returns 0 if not upper case at all, 1 if the first letter is upper case, 2 if all upper case */
    private int isUpper(String word) {
        boolean allUpper = true;
        boolean firstUpper = false;

        for(int i = 0; i < word.length(); i++) {

            char c = word.charAt(i);
            if (i == 0 && Character.isUpperCase(c)) {
                firstUpper = true;
            }
            if (!Character.isUpperCase(c)) {
                allUpper = false;
            }
        }

        if (allUpper)
            return 2;
        else if (firstUpper)
            return 1;
        else
            return 0;
    }

    private void addFeatureFromDataLoader(ArrayList<Pair<String, String>> features, String item, String featureName) {
        if (!this.dataLoaders.isEmpty()) {
            for (String key: this.dataLoaders.keySet()) {
                ListDataLoader val = this.dataLoaders.get(key);
                if (val.contains(item.toUpperCase()))
                    features.add(new Pair<>(featureName + key, "1"));
            }
        }
    }

    private ArrayList<Pair<String, String>> generateFeatureVector(String curWord, String prevWord, String nextWord) {

        String[] curWordItems = curWord.split("\t");

        ArrayList<Pair<String, String>> features = new ArrayList<>();

        features.add(new Pair<>("POS", curWordItems[1]));
//        features.add(new Pair<>("BIO[0]", Character.toString( curWordItems[2].charAt(0))));
        features.add(new Pair<>("BIO", curWordItems[2]));
        features.add(new Pair<>("CASE", Integer.toString(isUpper(curWordItems[0]))));

//        addFeatureFromDataLoader(features, curWordItems[0], "curWord");

        if (prevWord != null) {

            String[] prevWordItems = prevWord.split("\t");
            features.add(new Pair<>("prevWordPOS", prevWordItems[1]));
//            addFeatureFromDataLoader(features, prevWordItems[0], "prevWord");
//            features.add(new Pair<>("prevWordBIO[0]", Character.toString( prevWordItems[2].charAt(0))));
            features.add(new Pair<>("prevCASE", Integer.toString(isUpper(prevWordItems[0]))));
            features.add(new Pair<>("prevWordBIO", curWordItems[2]));

            if (prevWordItems.length > 3) {
                features.add(new Pair<>("prevWordTag", prevWordItems[3]));
            } else {
                features.add(new Pair<>("prevWordTag", "@@"));
            }

//            addFeatureFromDataLoader(features, prevWordItems[0], "pervWord");

        } else {

            features.add(new Pair<>("prevWordPOS", "NA"));
            features.add(new Pair<>("prevWordBIO", "NA"));
            features.add(new Pair<>("prevWordTag", "NA"));
            features.add(new Pair<>("prevCASE", "NA"));

        }

        if (nextWord != null) {

            String[] nextWordItems = nextWord.split("\t");
            features.add(new Pair<>("nextCASE", Integer.toString(isUpper(nextWordItems[0]))));
            features.add(new Pair<>("nextWordPOS", nextWordItems[1]));
//            features.add(new Pair<>("nextWordBIO[0]", Character.toString( nextWordItems[2].charAt(0))));
//            addFeatureFromDataLoader(features, nextWordItems[0], "nextWord");
            features.add(new Pair<>("nextWordBIO", nextWordItems[2]));

        } else {

            features.add(new Pair<>("nextWordPOS", "NA"));
            features.add(new Pair<>("nextWordBIO", "NA"));
            features.add(new Pair<>("nextCASE", "NA"));
        }



        return features;
    }

    public void buildFeatures() {
        try {
            Scanner myReader = new Scanner(this.file);
            String prevLine = null, nextLine = null, curLine = null;
            BufferedWriter bw = new BufferedWriter(new FileWriter(this.outFileName));
            if (myReader.hasNextLine())
                nextLine = myReader.nextLine();

            while (myReader.hasNextLine()) {

                prevLine = curLine;
                curLine = nextLine;
                if (myReader.hasNextLine())
                    nextLine = myReader.nextLine();
                else
                    nextLine = null;

                if (curLine == null || curLine.isEmpty()) {
                    bw.write("\n");
                    continue;
                }

                String cur = curLine, prev = null, next = null;
                if (prevLine != null && !prevLine.isEmpty())
                    prev = prevLine;

                if (nextLine != null && !nextLine.isEmpty())
                    next = nextLine;

                ArrayList<Pair<String, String>> featureVector = generateFeatureVector(cur, prev, next);
                if (featureVector != null) {

                    String[] items = cur.split("\t");
                    bw.write(items[0] + "\t");
                    for (Pair<String, String> pair : featureVector)
                        bw.write(pair.getKey() + "=" + pair.getValue() + "\t");

                    if (items.length > 3)
                        bw.write(items[3]);

                    bw.write( "\n");
                }
            }

            bw.write( "\n");
            myReader.close();
            bw.close();

        } catch (FileNotFoundException e) {

            System.out.println("An error occurred.");
            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println ("FeatureBuilder requires 2 argument:  trainFile, outputFile");
            System.exit(1);
        }

        String fileName = args[0];
        String outfileName = args[1];

        try {
            FeatureBuilder fb = new FeatureBuilder(fileName, outfileName);
            if (args.length > 2) {
                for (int i = 2; i < args.length; i++) {
                    String featureName = "feat_" + Integer.toString(i);
                    fb.addFeatureList(args[i], featureName);
                }
            }
            fb.buildFeatures();
        } catch (Exception ex) {
            System.out.println("file doesn't exist");
        }
    }
}
