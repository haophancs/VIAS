package com.haophan.vias.tools;

import android.content.Context;

import java.util.Arrays;
import java.util.Stack;

/**
 * Created by HaoPhan on 11/29/2017.
 */

public class Calculator {

    Context context;
    String currentLang;

    public Calculator(Context context){
        this.context = context;
        currentLang = new GetSharePrefs(context).getCurrentLang();
    }

    public String getResultOf (String s){

        String[] elementMath = preProcessing(s);
        try {
            elementMath = toPostFix(elementMath);
            Stack<String> stack = new Stack<>();
            for (int i = 0; i < elementMath.length; i++) {
                if (elementMath[i] == null || elementMath[i].equals("")) {
                    elementMath[i] = " ";
                }
            }
            for (int i = 0; i < elementMath.length; i++) {
                char c = elementMath[i].charAt(0);
                if (!isOperator(c)) stack.push(elementMath[i]);
                else {
                    double num = 0f;
                    double num1 = Float.parseFloat(stack.pop());
                    double num2 = Float.parseFloat(stack.pop());
                    switch (c) {
                        case '+':
                            num = num2 + num1;
                            break;
                        case '-':
                            num = num2 - num1;
                            break;
                        case '*':
                            num = num2 * num1;
                            break;
                        case '/':
                            if (num1 != 0) {
                                num = num2 / num1;
                            } else {
                                return currentLang.equals("vi")?
                                        "Biểu thức không hợp lệ, mẫu số phải khác 0"
                                        :"Math error, the divisor must be not equal to 0";
                            }
                            break;
                        case '%':
                            num = num2 % num1;
                            break;
                        case '^':
                            num = Math.pow(num2, num1);
                            break;
                    }
                    stack.push(Double.toString(num));
                }
            }
            Double result = Double.parseDouble(stack.pop());
            if (result - result.longValue() == 0) {
                return String.valueOf(result.longValue());
            }
            return String.valueOf(round(result, 2));
        } catch (Exception e){}
        return currentLang.equals("vi")?
                "Biểu thức không hợp lệ"
                :"Syntax error";
    }

    private static double round(double val, int places){

        if (places < 0) return val;
        long factor = (long) Math.pow(10, places);
        val = val * factor;
        return (double) Math.round(val) / factor;
    }

    private String[] preProcessing(String s){
        s = s.toLowerCase().trim()
                .replace("chia lấy dư", "%")
                .replace("chia lấy phần dư", "%")
                .replace("chia", "/")
                .replace("phần", "/")
                .replace("nhân", "*")
                .replace("mũ", "^")
                .replace("x", "*")
                .replace("trừ", "-")
                .replace("cộng", "+")
                .replace("mở ngoặc", "(")
                .replace("đóng ngoặc", ")")
                .replace("số pi", "3.14")
                .replace("số bi", "3.14")
                .replace("một", "1")
                .replace("hai", "2")
                .replace("ba", "3")
                .replace("bốn", "4")
                .replace("năm", "5")
                .replace("sáu", "6")
                .replace("bảy", "7")
                .replace("tám", "8")
                .replace("chín", "9")
                .replace("không", "0")
                .replace("chấm", ".")
                .replace("phẩy", ".")
                .replace(",", ".")
                .replace(" mươi",  "0")
                .replace(" chục",  "0")
                .replace(" trăm",  "00")
                .replace(" nghìn", "000")
                .replace(" triệu", "000000")
                .replace(" tỷ",    "000000000");

        s = s.toLowerCase().trim()
                .replace("divided by", "/")
                .replace("over", "/")
                .replace("times", "*")
                .replace("to the", "^")
                .replace("x", "*")
                .replace("minus", "-")
                .replace("plus", "+")
                .replace("opening bracket", "(")
                .replace("closed bracket", ")")
                .replace("pi", "3.14")
                .replace("pi number", "3.14")
                .replace("one", "1")
                .replace("two", "2")
                .replace("three", "3")
                .replace("four", "4")
                .replace("five", "5")
                .replace("six", "6")
                .replace("seven", "7")
                .replace("eight", "8")
                .replace("nine", "9")
                .replace("zero", "0")
                .replace("ten", "10")
                .replace("eleven", "11")
                .replace("twelve", "12")
                .replace("twenty", "20")
                .replace("dot", ".")
                .replace("point", ".")
                .replace(",", ".")
                //.replace("ty",  "0")
                //.replace(" chục",  "0")
                .replace(" hundred",  "00")
                .replace(" thousand", "000")
                .replace(" million", "000000")
                .replace(" billion", "000000000");

        s = s.replace("  ", "");

        for (int i = 0; i <= 9; i++){
            //s = s.replace("- "+i, "0 - "+i);
            s = s.replace("-"+i, "0 - "+i);
        }
        //s = s.replace("trừ", "-");
        s = s.replace("minus", "-");
        s = s.replaceAll("[^0-9().%*/+-^]", "");
        String s1 = "", elementMath[] = null;
        for (int i = 0; i < s.length(); i++){
            char c = s.charAt(i);
            if (!isOperator(c)){
                s1 += c;
            } else {
                s1 = s1 + " " + c + " ";
            }
        }
        s1 = s1.trim().replaceAll("\\s+", " ");
        elementMath = s1.split(" ");
        //System.out.println(s1);
        return elementMath;
    }

    private int getPriority(char op){
        if (op == '^')
            return 3;
        if (op == '*' || op == '/' || op == '%')
            return 2;
        if (op == '+' || op == '-')
            return 1;
        return 0;
    }

    private boolean isOperator(char c){
        char op[] = {'+', '-', '*', '/', '%', ')', '(', '^'};
        Arrays.sort(op);
        if (Arrays.binarySearch(op, c) > -1) {
            return true;
        }
        return false;
    }

    private boolean isOperand(String s){
        return s.matches("[^\\d+$|^([a-z]|[A-Z])$]");
    }

    private String[] toPostFix(String[] elementMath){
        String s1 = "", E[];
        Stack <String> S = new Stack <String>();
        for (int i=0; i<elementMath.length; i++){ 	// duyet cac phan tu
            char c = elementMath[i].charAt(0);	// c la ky tu dau tien cua moi phan tu

            if (!isOperator(c)) 		// neu c khong la toan tu
                s1 = s1 + " " + elementMath[i];		// xuat elem vao s1
            else{						// c la toan tu
                if (c == '(') S.push(elementMath[i]);	// c la "(" -> day phan tu vao Stack
                else{
                    if (c == ')'){			// c la ")"
                        char c1;		//duyet lai cac phan tu trong Stack
                        do{
                            c1 = S.peek().charAt(0);	// c1 la ky tu dau tien cua phan tu
                            if (c1 != '(') s1 = s1 + " " + S.peek(); 	// trong khi c1 != "("
                            S.pop();
                        }while (c1 != '(');
                    }
                    else{
                        while (!S.isEmpty() && getPriority(S.peek().charAt(0)) >= getPriority(c)){
                            // Stack khong rong va trong khi phan tu trong Stack co do uu tien >= phan tu hien tai
                            s1 = s1 + " " + S.peek();	// xuat phan tu trong Stack ra s1
                            S.pop();
                        }
                        S.push(elementMath[i]); // 	dua phan tu hien tai vao Stack
                    }
                }
            }
        }
        while (!S.isEmpty()){	// Neu Stack con phan tu thi day het vao s1
            s1 = s1 + " " + S.peek();
            S.pop();
        }
        E = s1.split(" ");	//	tach s1 thanh cac phan tu
        return E;
    }
}
