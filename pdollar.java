// The code is a translation of javascript implementation available at http://depts.washington.edu/acelab/proj/dollar/pdollar.js
// Makefile is referred from my git repository https://github.com/akshit913/Memory-resident-B-plus-tree/blob/master/Makefile
// some other references are :- 
// https://stackoverflow.com/questions/13195797/delete-all-files-in-directory-but-not-directory-one-liner-solution
import java.lang.*;
import java.util.ArrayList;
import java.io.*;
import java.nio.file.*;
class pdollar{
    public static void main(String[] args) throws Exception{
        if(args.length == 0){
            System.out.println("Welcome to the help screen!");
            System.out.println("pdollar –t <gesturefile> ");
            System.out.println("Adds the gesture file to the list of gesture templates.");
            System.out.println();
            System.out.println("pdollar -r");
            System.out.println("Clears the templates.");
            System.out.println();
            System.out.println("pdollar <eventstream>");
            System.out.println("Prints the name of gestures as they are recognized from the event stream.");
            System.out.println();
        }else if(args.length == 1){
            if(args[0].equals("-r")){
                delete();
            }else{
                recognize(args[0]);
            }
        }else if(args.length == 2){
            if(args[0].equals("-t")){
                addGesture(args[1]);
            }else{
                System.out.println("Invalid command. Run pdollar for help.");
            }
        }else{
            System.out.println("Invalid command. Run pdollar for help.");
        }
    }
    public static void addGesture(String filename) throws Exception{
        File file = new File(System.getProperty("user.dir") + "/templates");
        boolean bool = file.mkdir();
        Path dest = Paths.get(System.getProperty("user.dir") + "/templates/" + filename);
        File src  = new File(System.getProperty("user.dir")+"/gestureFiles/" + filename);
        InputStream inpsrc = new FileInputStream(src);
        Files.copy(inpsrc,dest,StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Gesture successfully added!");
    }
    public static void recognize(String eventfilename) throws Exception{
        ArrayList<PointCloud> pc = new ArrayList<>();
        File path = new File(System.getProperty("user.dir") + "/templates");
        File [] files = path.listFiles();
        for (int i = 0; i < files.length; i++){
            if (files[i].isFile()){ //this line weeds out other directories/folders
                ArrayList<Point> points = new ArrayList<>();
                String name;
                int id = 0;
                BufferedReader br = new BufferedReader(new FileReader(files[i])); 
                String st;
                name = br.readLine(); 
                while ((st = br.readLine()) != null){
                    if(st.equals("BEGIN")){
                        id++;
                    }else if(st.equals("END")){

                    }else{
                        String[] s = br.readLine().split(",");
                        if(s.length==2){
                            points.add(new Point(Integer.parseInt(String.valueOf(s[0])),Integer.parseInt(String.valueOf(s[1])),id-1));
                        }
                        
                    }
                }
                pc.add(new PointCloud(name,points));
            }
        }
        File event = new File(System.getProperty("user.dir") + "/eventfiles/" + eventfilename);
        BufferedReader br1 = new BufferedReader(new FileReader(event)); 
        String st1;
        int id1 = 0;
        ArrayList<Point> points2 = new ArrayList<>();
        while ((st1 = br1.readLine()) != null){
            if(st1.equals("MOUSEDOWN")){
                id1++;
            }else if(st1.equals("MOUSEUP")){

            }else if(st1.equals("RECOGNIZE")){
                PDollarRecognizer p = new PDollarRecognizer(pc);
                p.recognize(points2);
                points2 = new ArrayList<>();
            }else{
                String[] s1 = br1.readLine().split(",");
                if(s1.length == 2){
                    points2.add(new Point(Integer.parseInt(String.valueOf(s1[0])),Integer.parseInt(String.valueOf(s1[1])),id1-1));
                }
            }
        }
        
    } 
    public static void delete(){
        File dir = new File(System.getProperty("user.dir") + "/templates/");
        for(File file: dir.listFiles()) 
        if (!file.isDirectory()) 
            file.delete();
        System.out.println("Template cleaned!");
        }
        
}
class Point{
    double x;
    double y;
    double id;
    public Point(double x, double y, double id){
        this.x = x;
        this.y = y;
        this.id = id;
    }
}
class PointCloud{
    String name;
    ArrayList<Point> points = new ArrayList<>();
    int Numpoints = 32;
    Point Origin = new Point(0,0,0);
    public PointCloud(String name,ArrayList<Point> points){
        this.name = name;
        this.points = resample(points,Numpoints);
        this.points = scale(this.points);
        this.points = translateTo(this.points,Origin);
    }
    public static double pathLength(ArrayList<Point> points){
        double d = 0;
        for(int i = 1;i<points.size();i++){
            if(points.get(i).id == points.get(i-1).id){
                d += distance(points.get(i-1), points.get(i));
            }
        }
        return d;
    }
    public static double distance(Point p1,Point p2){
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        return Math.sqrt(dx*dx + dy*dy);
    }
    public static Point centroid(ArrayList<Point> points){
        double x = 0;
        double y = 0;
        for(int i = 0;i<points.size();i++){
            x += points.get(i).x;
            y += points.get(i).y;
        }
        x /= points.size();
        y /= points.size();
        return new Point(x,y,0);
    }
    public static ArrayList<Point> translateTo(ArrayList<Point> points,Point pt){
        Point c = centroid(points);
        ArrayList<Point> newpoints = new ArrayList<>();
        for(int i = 0;i<points.size();i++){
            double qx = points.get(i).x + pt.x - c.x;
            double qy = points.get(i).y + pt.y - c.y;
            newpoints.add(new Point(qx,qy,points.get(i).id)); 
        }
        return newpoints;
    }
    public static ArrayList<Point> scale(ArrayList<Point> points){
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for(int i = 0;i<points.size();i++){
            minX = Math.min(minX, points.get(i).x);
            minY = Math.min(minY, points.get(i).y);
            maxX = Math.max(maxX, points.get(i).x);
            maxY = Math.max(maxY, points.get(i).y);
        }
        double size = Math.max(maxX - minX, maxY - minY);
        ArrayList<Point> newpoints = new ArrayList<>();
        for(int i = 0;i<points.size();i++){
            double qx = (points.get(i).x - minX) / size;
            double qy = (points.get(i).y - minY) / size;
            newpoints.add(i,new Point(qx,qy,points.get(i).id)); 
        }
        return newpoints;
    }
    public static ArrayList<Point> resample(ArrayList<Point> points, int n){
        double I = pathLength(points) / (n-1);
        double D = 0;
        ArrayList<Point> newpoints = new ArrayList<>();
        newpoints.add(0, new Point(points.get(0).x, points.get(0).y,points.get(0).id));
        for(int i = 1;i<points.size();i++){
            if(points.get(i).id == points.get(i-1).id){
                double d = distance(points.get(i-1), points.get(i));
                if ((D + d) >= I)
                {
                    double qx = points.get(i-1).x + ((I - D) / d) * (points.get(i).x - points.get(i-1).x);
                    double qy = points.get(i-1).y + ((I - D) / d) * (points.get(i).y - points.get(i-1).y);
                    Point q = new Point(qx, qy, points.get(i).id);
                    newpoints.add(q); // append new point 'q'
                    points.add(i,q); // insert 'q' at position i in points s.t. 'q' will be the next i
                    D = 0;
                }
                else D += d;
            }
        }
        if (newpoints.size() == n - 1) // sometimes we fall a rounding-error short of adding the last point, so add it if so
		    newpoints.add(newpoints.size(),new Point(points.get(points.size() - 1).x, points.get(points.size() - 1).y, points.get(points.size() - 1).id));
	    return newpoints;
    }

}
class PDollarRecognizer{
    ArrayList<PointCloud> PointClouds= new ArrayList<>();
    
    public PDollarRecognizer(ArrayList<PointCloud> PointClouds){
        this.PointClouds = PointClouds;
        
    }
    public void recognize(ArrayList<Point> points){
        PointCloud candidate = new PointCloud("", points);
        int u = -1;
        double b = Double.MAX_VALUE;
        for(int i = 0;i < this.PointClouds.size();i++){
            double d = GreedyCloudMatch(candidate.points, this.PointClouds.get(i));
            if(d < b){
                b = d;
                u = i;
            }
        }
        if(u == -1){
            System.out.println("no match found");
        }else{
            System.out.println(this.PointClouds.get(u).name);
        }
    }
    public int AddGesture(String name, ArrayList<Point> points){
        this.PointClouds.add(new PointCloud(name, points));
        int num = 0;
        for(int i = 0;i<this.PointClouds.size();i++){
            if(this.PointClouds.get(i).name == name){
                num++;
            }
        }
        return num;
    } 
    public static double GreedyCloudMatch(ArrayList<Point> points1, PointCloud points2){
        double e = 0.5;
        int step = (int)Math.floor(Math.pow(points1.size(), 1.0-e));
        double min = Double.MAX_VALUE;
        for(int i = 0;i<points1.size();i+=step){
            double d1 = cloudDistance(points1, points2.points, i);
            double d2 = cloudDistance(points2.points, points1, i);
            min = Math.min(min,Math.min(d1,d2));
        }
        return min;
    }
    public static double cloudDistance(ArrayList<Point> pts1, ArrayList<Point> pts2, int start){
        boolean[] matched = new boolean[pts1.size()];
        for(int k = 0;k<matched.length;k++){
            matched[k] = false;
        }
        int i = start;
        double sum = 0;
        do{
            int index = -1;
            double min = Double.MAX_VALUE;
            for(int j = 0;j<matched.length;j++){
                if(!matched[j]){
                    double d = distance(pts1.get(i), pts2.get(i));
                    if(d < min){
                        min = d;
                        index = j;
                    }
                }
            }
            matched[index] = true;
            double weight = 1 - ((i - start + pts1.size()) % pts1.size()) / pts1.size();
            sum += weight * min;
		    i = (i + 1) % pts1.size();
        }while(i!=start);
        return sum;
    }
    public static double distance(Point p1,Point p2){
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        return Math.sqrt(dx*dx + dy*dy);
    }
}