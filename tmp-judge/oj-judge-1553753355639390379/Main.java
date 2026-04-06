import java.util.Scanner;

// 用户的代码
class Solution {
    public int add(int a, int b) {
        return a + b;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int a = sc.nextInt();
        int b = sc.nextInt();
        sc.close();
        
        Solution sol = new Solution();
        System.out.println(sol.add(a, b));
    }
}