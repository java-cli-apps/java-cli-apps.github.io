create table commandes (
    id            uuid not null constraint "commandes_pk" primary key,
    numero_client varchar,
    date_commande timestamp,
    montant       integer
);
