/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package proje2;

/**
 * @author Amine Ceyda Abanik
 *   1721221024 - FSMVÜ
 */

import java.io.*;
import java.net.*;
import java.util.*;

class ChatServer {
    
    ServerSocket serverSock;// bağlantı için sunucu soketi
    static Boolean running = true;  //sunucunun istemcileri kabul edip etmediğini kontrol eder
    static String[][] messages = new String[1000000][3]; //mesaj, gönderen, alıcı (boş alıcı = herkese yayın)
    
    static ArrayList<String>users = new ArrayList<String>();
    static int nextIndex = 0;
   
    /** Main
      * @param args parameters from command line
      */
    public static void main(String[] args) { 
        for(int i=0; i<1000000; i++) {
            messages[i][0] = "";
            messages[i][1] = "";
            messages[i][2] = "";
        }
        new ChatServer().go(); //server baslar
    }
    
    /** Go
      * server ı başlatır
      */
    public void go() { 
        System.out.println("İstemci baglantisi bekleniyor..");
        
        Socket client = null;//istemci bağlantısını tutar
        
        try {
            serverSock = new ServerSocket(5000);  //sunucuya bir bağlantı noktası atar
            
            while(running) {  //birden fazla kullanıcıyı kabul etmek için döngü başlatır
                client = serverSock.accept();  //bağlanmasını bekler
                System.out.println("İstemciye baglanildi.");
                
                Thread t = new Thread(new ConnectionHandler(client)); //yeni istemci için bir tread oluşturur ve sokete iletir
                t.start(); //yeni tread başlar
            }
        }catch(Exception e) { 
          
            try {
                client.close();
            }catch (Exception e1) { 
                System.out.println("Soket kapatilamadi");
            }
            System.exit(-1);
        }
    }
    
    //***** iç sınıf istemci bağlantısı için tread
    class ConnectionHandler implements Runnable { 
        private PrintWriter output; //ağ akışına yazıcı atar
        private BufferedReader input; //ağ girişi için akış
        private Socket client;  //istemci soketinin kaydını tutar
        private boolean running;

        /* ConnectionHandler
         * Constructor
         * @param bu istemci bağlantısına ait soket
         */    
        ConnectionHandler(Socket s) { 
            this.client = s;  //constructor kullanıcı atar  
            try {  //tüm bağlantıları istemciye atmak için
                this.output = new PrintWriter(client.getOutputStream());
                InputStreamReader stream = new InputStreamReader(client.getInputStream());
                this.input = new BufferedReader(stream);
            }catch(IOException e) {
                e.printStackTrace();        
            }            
            running=true;
        } 
        
        
        /* run
         * tread başladığında başlar
         */
        public void run() {
            
            
            boolean gaveUsername = false;
            String username = "";
            String msg="";
            int msgIndex = 0;
            ArrayList<String>visibleUsers = new ArrayList<String>();
            
            //kullanıcı adı sorusu için 
            while(!gaveUsername) {
                try {
                    if (input.ready()) { 
                        username = input.readLine();
                        gaveUsername = true;
                        users.add(username);
                        
                    }
                }catch (IOException e) { 
                    System.out.println("İstemciden kullanıcı adı alınamadı");
                    e.printStackTrace();
                } 
            }
            System.out.println(username+" baglanildi.");
            
            //bağlı kullanıcılar listesi
             synchronized(users) {
                 String list = "";
                for(int i=0; i<users.size(); i++) {
                    if(!users.get(i).equals(username)) {
                       list += users.get(i)+"/";
                      
                       
                        visibleUsers.add(users.get(i));
                    }
                }
               output.println(list);
               output.flush();
            }
            
            
            while(running) {  
                synchronized(users) {
                    
                    //yeni bağlanan kişiler güncellenir
                    for(int i=users.indexOf(username)+1; i<users.size(); i++) {
                        if(!visibleUsers.contains(users.get(i))) {
                            output.println("/new/"+users.get(i));
                            output.flush();
                            visibleUsers.add(users.get(i));
                        }
                    }
                    
                    //ayrılan kişiler güncellenir
                    for(int i=0; i<visibleUsers.size(); i++) {
                        if(!users.contains(visibleUsers.get(i))) {
                            output.println("/delete/"+visibleUsers.get(i));
                            output.flush();
                            visibleUsers.remove(visibleUsers.get(i));
                            i--;
                           
                        }
                    }
                }
                
                //Okunmamış mesajları yayınlar
                if(msgIndex < nextIndex) {
                    if(!messages[msgIndex][1].equals(username)) {
                       
                        //Bu kullanıcıya doğrudan mesaj
                        if(messages[msgIndex][2].equals(username)) {
                            output.println("/dm/"+messages[msgIndex][1]+"/"+messages[msgIndex][0]);
                             output.flush();
                            
                        } else if(messages[msgIndex][2].equals("")){
                            System.out.println(username+" hears: "+messages[msgIndex][0]);
                            //genele mesaj
                            output.println("/general/"+messages[msgIndex][1]+"/"+messages[msgIndex][0]);
                             output.flush();
                        }
                        
                       
                       
                    }
                    msgIndex++;
                    
                }
                try {
                    if (input.ready()) { //gelen mesajı kontrol eder
                        synchronized(messages) {
                            msg = input.readLine();  //kullanıcıdan mesaj alır
                            if(msg.equals("/quit")) {
                                
                                running = false;
                            } else if(msg.substring(0,3).equals("/dm")) {
                                
                                int slashIndex = msg.indexOf("/",4);
                                String reciever = msg.substring(4,slashIndex);
                                String actualMsg = msg.substring(slashIndex+1,msg.length());
                                messages[nextIndex][0] = actualMsg;
                                messages[nextIndex][1] = username;
                                messages[nextIndex][2] = reciever;
                                nextIndex++;
                            } else {
                                
                             
                                int slashIndex = msg.indexOf("/",9);
                                 String reciever = msg.substring(9,slashIndex);
                                String actualMsg = msg.substring(slashIndex+1,msg.length());
                                messages[nextIndex][0] = actualMsg;
                                messages[nextIndex][1] = username;
                                nextIndex++;
                            }
                            
                            
                        }
                        
                    }
                }
                catch (IOException e) { 
                    System.out.println("İstemciden mesaj alınamadı");
                    e.printStackTrace();
                }
            }
            
            
            try {
                System.out.println(username+" ayrildi.");
                input.close();
                output.close();
                client.close();
                users.remove(username);
            }catch (Exception e) { 
                System.out.println("Soket kapatılamadı");
            }
        } // run() bitti
    } // inner class  bitti 
} //chatserver class bitti