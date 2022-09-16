package com.cursoandroid.uber.helper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ConfiguracaoFirebase {
    private static DatabaseReference referenciaDatabase;
    private static FirebaseAuth referenciaAutenticacao;

    //Retorna a instância do database
    public static DatabaseReference getFirebase(){
        if (referenciaDatabase == null){
            referenciaDatabase = FirebaseDatabase.getInstance().getReference();
        }
        return referenciaDatabase;
    }

    //Retorna a instância do firebaseAuth
    public static FirebaseAuth getAutenticacao(){
        if (referenciaAutenticacao == null){
            referenciaAutenticacao = FirebaseAuth.getInstance();
        }
        return referenciaAutenticacao;
    }

}


