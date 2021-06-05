/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package proje2;

/**
 * @author Amine Ceyda Abanik
 *   1721221024- FSMVÜ
 */
//importlar
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.BorderLayout;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.Socket;

import javax.swing.Timer;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;

import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;

import java.util.ArrayList;

//Main Class
class ChatClient {
    
    //GUI
    private JFrame window, settingsTab, chatTab,serverMessageWindow;
    private JButton sendButton, clearButton,loginInButton,settingsButton, confirmButton, logOutButton;
    private JLabel introLabel, usernameLabel, portLabel, addressLabel, hubIntroLabel, onlineLabel,guidelineLabel, errorLabel;
    private JTextField typeField, loginField, portField, addressField,serverMsgField;
    private JTextArea serverMsgArea;  
    private JPanel mainPanel, southPanel, hubPanel; 
    //ağ
    private Socket mySocket; //bağlantı için soket
    private BufferedReader input; // ağ akışı için okuyucu
    private PrintWriter output;  //ağ çıkışı için yazıcı
    //kullanıcı girişleri
    private String inputAddress, inputName; //İstemci kullanıcı adı ve sunucuya ip adresi
    private int inputPort; //port
    //Booleans
    private boolean load = true;  //kişinin giriş yapıp yapmadığını kontrol eder
    private boolean running = true;  //sunucudan gelen mesajların gelmesine izin verir
    //ArrayLists
    private ArrayList<String> online; 
    private ArrayList<String> onlineChats;
    private ArrayList<JTextArea> messageAreas; 
    private ArrayList<JTextField> typeFields;
    private ArrayList<JButton> userButtons;
    private ArrayList<JFrame> chatTabs;
    private ArrayList<String> savedMessages; // daha önce kapatmış olmanız durumunda, DM'lerden gelen kayıtlı mesajları tutar
    private String savedServerMessages; //genel sohbetten kaydedilmiş mesajları tutar
    //Threads
    private Thread hubThread;
  
    // Main Method
    public static void main(String[] args) { 
      new ChatClient().go();
    }
  
    /*
     * Method: go ()
     * Description: sohbet istemcisini başlatır.
     * @param: null
     * @return: null
     */
    public void go() {
        //Pencere açılıyor
        window = new JFrame("CHAT");
        //server Mesaj Penceresi daha sonra tanımlanacak.
        serverMessageWindow = null; 
        
        //Panels
        mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
        // Sohbet Panelinin daha sonra tanımlanacak kısmı
        southPanel = new JPanel();
        southPanel.setLayout(new GridLayout(2,0));
        
        //port ve adres için varsayılan ayarlar
        inputPort = 5000;
        inputAddress = "127.0.0.1";
        
        //istemcileri tutan dizi listeleri
        online = new ArrayList<String>();
        onlineChats = new ArrayList<String>();
        messageAreas = new ArrayList<JTextArea>();
        typeFields = new ArrayList<JTextField>();
        chatTabs = new ArrayList<JFrame>();
        savedMessages = new ArrayList<String>();
        savedServerMessages = "";
        
      
        loginInButton = new JButton("GİRİS");
        loginInButton.addActionListener(new LogInButtonListener());
      
        
        
        introLabel = new JLabel("Hoşgeldiniz! Lütfen kullanıcı adı giriniz.");
        usernameLabel = new JLabel("Kullanıcı adı:");
       
        guidelineLabel = new JLabel("Lütfen kullanıcı adlarının şunları içeremeyeceğini unutmayın:'/'.");
        errorLabel = new JLabel("");
        
        
        loginField = new JTextField(30);
        
        
        
        mainPanel.add(introLabel);
        mainPanel.add(guidelineLabel);
        mainPanel.add(usernameLabel);
        mainPanel.add(loginField);
        mainPanel.add(loginInButton);
       

        //set visible.
        window.add(mainPanel);
        window.setSize(400,400);
        window.setVisible(true);
    }
    
    /*
     * Method: connect()
     * Description: 
        Sunucuya bağlanmaya çalışır ve soketi ve akışları oluşturur
     
     */
    public Socket connect(String ip, int port) { 
        System.out.println("Baglanti kurmaya çalışılıyor..");
      
        try {
            mySocket = new Socket(ip, port); //soket bağlantısını deneyin (yerel adres). Bu, bağlantı kurulana kadar bekleyecek
        
            InputStreamReader stream1= new InputStreamReader(mySocket.getInputStream()); //Ağ girişi için akış
            input = new BufferedReader(stream1);     
            output = new PrintWriter(mySocket.getOutputStream()); //ağ akışına yazıcı atar
        } catch (IOException e) {  //bağlantı hatası oluştu
            load = false; 
            System.out.println("Sunucu Bağlantısı Başarısız!!!!");
            e.printStackTrace();
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING)); //closes program if connection fails.
        }
        System.out.println("BAGLANDİ..");
        return mySocket;
    } 
    
    
    /*
     * Method: readMessagesFromServer()
     * Description: sunucu girişini bekler 
     *(bir Direkt Mesaj, Genel Mesaj, Yeni Kullanıcı Giriş Yaptı veya Kullanıcı çıkış yaptı)
     * ve ardından kullanıcı arayüzünü günceller. 
     *GUI'ye müdahale etmemek için bir döngü kullanmak yerine arada bir çağrılır.
     
     */
    public void readMessagesFromServer() { 
      
        if(running) {  
            try {
                if (input.ready()) { 
                    String msg;
                    msg = input.readLine(); 
            
                    //Mesajın DM olup olmadığını kontrol eder
                    if (msg.substring(0,3).equals("/dm")){
                        int slashIndex = msg.indexOf("/",4);
                        String sender = msg.substring(4,slashIndex); //DM'nin göndericisini alır.
                        String actualMsg = msg.substring(slashIndex+1,msg.length()); //DM içeriğini alır.
                      
                        if (onlineChats.indexOf(sender) == -1){ //DM içeriğini alır.
                            ChatPanel c = new ChatPanel(sender); //Yeni panel
                        
                            for (int i = 0; i < messageAreas.size();i++){ 
                                String s = messageAreas.get(i).getText();
                                if ( (s).substring(35,s.indexOf("\n")).equals(sender)) {
                                    messageAreas.get(i).append(sender+":"+actualMsg+"\n");
                                    i = messageAreas.size();
                                } 
                            }
                        
                        } else { //Sohbet penceresi zaten açıksa.
                            for (int i = 0; i < messageAreas.size();i++){ 
                                String s = messageAreas.get(i).getText();
                                if ( (s).substring(35,s.indexOf("\n")).equals(sender)) {
                                    messageAreas.get(i).append(sender+":"+actualMsg+"\n");
                                    i = messageAreas.size();
                                } 
                            }
                        
                        }
                    } else if (msg.substring(0,4).equals("/new")){ //eğer sunucu mesajı yeni bir kullanıcının oturum açtığını söylüyorsa.
                        String newUser = msg.substring(5); 
                      
                       
                        JButton newUserButton = new JButton(newUser); 
                        newUserButton.addActionListener(new ChatButtonListener());
                    
                        
                        userButtons.add(newUserButton);
                        hubPanel.add(newUserButton);
                    
                        
                        hubPanel.revalidate();
                        hubPanel.repaint();
                      
                    } else if (msg.substring(0,7).equals("/delete")){ //sunucu mesajı eski bir kullanıcının oturumu kapattığını söylüyorsa.
                      
                        String oldUser = msg.substring(8); 
                        for (int i = 0; i < userButtons.size(); i++){ 
                            if (userButtons.get(i).getText().equals(oldUser)){
                                chatTabs.get(i).dispose();
                                messageAreas.remove(i);
                                typeFields.remove(i);
                                userButtons.get(i).setVisible(false);  
                                userButtons.remove(i); //arraylistten sil
                                i = userButtons.size();
                            }
                        }
                    
                        
                        hubPanel.revalidate();
                        hubPanel.repaint();
                      
                    } else if (msg.substring(0,8).equals("/general")){ //sunucu mesajı genel bir sohbet mesajıysa

                        if (serverMessageWindow == null){ //generalChat açılmadıysa, oluşturun.
                            new ServerChatFrame();
                        } 
                        int slashIndex = msg.indexOf("/",9);
                        String sender = msg.substring(9,slashIndex); //gönderenin adını alın.
                        String actualMsg = msg.substring(slashIndex+1); //asıl mesajı alın.
                        serverMsgArea.append(sender+":"+actualMsg+"\n"); 
                    }
                }                                 
            } catch (IOException e) { 
                 System.out.println("Sunucudan mesaj alınamadı");
                 e.printStackTrace();
            }
        } else { //if running == false
            try {  //ana döngüden çıktıktan sonra tüm soketleri kapatmamız gerekiyor
                input.close();
                output.close();
                mySocket.close();
            } catch (Exception e) { 
                System.out.println("Soket kapatılamadı");
            }
        }
    } 
    
    
    //****** Inner Classes for Action Listeners ****
    
    // GÖNDER BUTONU
    class SendButtonListener implements ActionListener { 
        String name = null; 
        public void actionPerformed(ActionEvent event)  {
            if (name == null){ 
                serverMsgArea.append("You:"+serverMsgField.getText()+"\n"); 
          
                output.println("/general/"+inputName+"/"+serverMsgField.getText()); 
                output.flush();
                serverMsgField.setText(""); //RESET TEXTFIELD
            } else {
          
                for (int i = 0; i < messageAreas.size();i++){ //JTextAreas dizi listesine bakar
                    String s = messageAreas.get(i).getText();
                                     
                    if ( (s).substring(35,s.indexOf("\n")).equals(name)) { //uygun mesaj alanını bulun.
                        messageAreas.get(i).append("You:"+ typeFields.get(i).getText()+"\n"); 
                        output.println("/dm/"+name+"/"+typeFields.get(i).getText()); //mesajı servera yollar
                        output.flush();
                        typeFields.get(i).setText(""); //reset textfield.
                        i = messageAreas.size();
                    } 
                }
            }
        }
    } 
    
    //SİLME BUTONU
    class ClearButtonListener implements ActionListener { 
        String name = null; 
      
        public void actionPerformed(ActionEvent event)  {
            if (name == null){ 
                serverMsgField.setText("");
            }
        
            for (int i = 0; i < messageAreas.size();i++){ 
                String s = messageAreas.get(i).getText();
                if ( (s).substring(35,s.indexOf("\n")).equals(name)) {
                    typeFields.get(i).setText("");  //reset textfield.
                } 
            }
        }     
    } 
    
    //GİRİŞ BUTONU
    class LogInButtonListener implements ActionListener { 
        public void actionPerformed(ActionEvent event)  {
            
            inputName = loginField.getText();
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING)); //close starting window.
            //connect server
            connect(inputAddress, inputPort);
            
            output.println(inputName);
            output.flush();
            
            String userNames="";
            boolean gotUserNames = false;
            while (!gotUserNames){ 
                try {
                    if (input.ready()){
                        userNames = input.readLine();
                        gotUserNames = true;
                    }
                } catch (IOException e){
                    System.out.println("Kullanıcı adları alınamadı.");
                };
            }
        
            int oldPosition = 0;
            for (int i = 0 ; i < userNames.length(); i++){
                if (userNames.charAt(i) == '/'){ 
                    online.add(userNames.substring(oldPosition,i)); //online kullanıcıları arrayliste ekler
                    oldPosition = i+1; 
                } else if (i == userNames.length()-1){
                    online.add(userNames.substring(oldPosition,i+1)); 
                }
            }
           
            hubThread = new Thread(new HubThread());
            hubThread.start();
        }     
    }
    
    
    // ChatButtonListener
    class ChatButtonListener implements ActionListener{ 
        public void actionPerformed(ActionEvent event)  {
            String openChat = event.getActionCommand(); 
            boolean check = true;
            for (int i = 0; i < onlineChats.size(); i++){
                if (onlineChats.get(i) == openChat){ 
                    check = false;
                    i = onlineChats.size();
                }
            }
            if (check == true){ 
                ChatPanel c = new ChatPanel(openChat);
            }
        }
    } 
    
    //Server chat buton
    class ServerChatButtonListener implements ActionListener{ 
        public void actionPerformed(ActionEvent event)  {
            new ServerChatFrame();
        }
    } 
    //ÇIKIŞ BUTONU
    class LogOutButtonListener implements ActionListener{ 
        public void actionPerformed(ActionEvent event)  {
            output.println("/çıkış");
            output.flush();
            try {
                System.exit(0);
            } catch(Exception e){};
        }
    }
    
    //****** Inner Classes for JFrames ****
    public class ServerChatFrame {
        // constructor 
        public ServerChatFrame(){
            if (serverMessageWindow == null){ 
                serverMessageWindow = new JFrame("Server Chat");
               
                serverMsgArea = new JTextArea();
               
                if (savedServerMessages.equals("")){
                    serverMsgArea.append("Bu, sunucuyla sohbetinizin başlangıcı!\n");
                } else {
                    serverMsgArea.append(savedServerMessages);
                }
                
                
                serverMessageWindow.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e){
                        savedServerMessages = serverMsgArea.getText();
                        serverMessageWindow.dispose();
                        serverMessageWindow = null;
                    }
                });
          
                
                JScrollPane scroll = new JScrollPane(serverMsgArea,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
          
                
                clearButton = new JButton("SİL");
                ClearButtonListener c = new ClearButtonListener();
                clearButton.addActionListener(c);
                sendButton = new JButton("GÖNDER");
                SendButtonListener s = new SendButtonListener();
                sendButton.addActionListener(s);
                
                
                JPanel southPanel = new JPanel();
                serverMsgField = new JTextField(20);
                southPanel.add(serverMsgField);
                southPanel.add(sendButton);
                southPanel.add(errorLabel);
                southPanel.add(clearButton);
               
                serverMessageWindow.add(BorderLayout.CENTER,scroll);
                serverMessageWindow.add(BorderLayout.SOUTH,southPanel);
                serverMessageWindow.setSize(400,400);
                serverMessageWindow.setVisible(true);
                serverMessageWindow.setLocation(800,0);
            }
        }
    } 
    
    //ÖZEL DM
    public class ChatPanel {
        public ChatPanel(String name) {
            String openChat = name;
            //message area
            JTextArea msgArea = new JTextArea();
            boolean check = true;
            for (int i = 0; i < savedMessages.size(); i++){
                if ((savedMessages.get(i)).substring(35,savedMessages.get(i).indexOf("\n")).equals(name)){
                    check = false;
                    msgArea.append(savedMessages.get(i));
                    i = savedMessages.size();
                }
            }
            if (check == true){
                msgArea.append("Bu, sohbetinizin başlangıcı.." );
                msgArea.append("\n");
            }
            messageAreas.add(msgArea);
            //scrollbar
            JScrollPane scroll = new JScrollPane(msgArea,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
           
            JFrame chatTab = new JFrame(openChat);
            chatTabs.add(chatTab);
            chatTab.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e){
                    onlineChats.remove(openChat);  
                    for (int i = 0; i < messageAreas.size(); i++){
                        String s = messageAreas.get(i).getText();
                        if ((s).substring(35,s.indexOf("\n")).equals(openChat)){ 
                            String oldMessages = messageAreas.get(i).getText();
                            for (int j = 0; j < savedMessages.size(); j++){
                                if (savedMessages.get(j).substring(35,s.indexOf("\n")).equals(openChat)){
                                    savedMessages.remove(j);
                                }
                            }
                            savedMessages.add(oldMessages);
                            System.out.println(oldMessages);
                            messageAreas.remove(i); 
                            typeFields.remove(i);
                            i = messageAreas.size();
                        }
                    }  
                }
            });
            
            clearButton = new JButton("SİL");
            ClearButtonListener c = new ClearButtonListener();
            c.name = openChat;
            clearButton.addActionListener(c);
            sendButton = new JButton("GÖNDER");
            SendButtonListener s = new SendButtonListener();
            s.name = openChat;
            sendButton.addActionListener(s);
          
            
            JPanel southPanel = new JPanel();
            JTextField typeField = new JTextField(20);
            typeFields.add(typeField);
            southPanel.add(typeField);
            southPanel.add(sendButton);
            southPanel.add(errorLabel);
            southPanel.add(clearButton);
            
            chatTab.add(BorderLayout.CENTER,scroll);
            chatTab.add(BorderLayout.SOUTH,southPanel);
            chatTab.setSize(400,400);
            chatTab.setVisible(true);
            chatTab.setLocation(800,0);   
            onlineChats.add(openChat);
        }
    } 
    
    
    public class HubThread implements Runnable{
        JFrame thisFrame = new JFrame(); 
      
        public void run(){
            if (load == true){ 
                thisFrame.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e){
                        output.println("/ÇIKIŞ"); 
                        output.flush();
                        System.exit(0);
                    }
                });
                //create new JPanel
                hubPanel = new JPanel();
                //create jlabels and jbuttons
                hubIntroLabel = new JLabel("Merhaba, "+inputName+"\n");
                onlineLabel = new JLabel("Online:");
                JButton logOutButton = new JButton("ÇIKIŞ");
                logOutButton.addActionListener(new LogOutButtonListener());
                JButton serverChatButton = new JButton("SERVER CHAT");
                serverChatButton.addActionListener(new ServerChatButtonListener());
          
                
                hubPanel.add(hubIntroLabel);
                hubPanel.add(logOutButton);
                hubPanel.add(serverChatButton);
                hubPanel.add(onlineLabel);
                hubPanel.setLayout(new BoxLayout(hubPanel, BoxLayout.Y_AXIS));
                
               
                JScrollPane scroll = new JScrollPane(hubPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                
                
                userButtons = new ArrayList<JButton>();
                for (int i = 0; i < online.size(); i++){
                    JButton newOne = new JButton(online.get(i));
                    newOne.addActionListener(new ChatButtonListener());
                    hubPanel.add(newOne);
                    userButtons.add(newOne);
                }
                
                
                thisFrame.add(scroll);
                thisFrame.setSize(400,400);
                thisFrame.setVisible(true);
                
                Timer timer = new Timer(100, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        readMessagesFromServer();
                    }
                });
                timer.start();
            }
        }
    } 
} 