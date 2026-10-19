import { useCallback, useEffect, useState } from "react";
import { creerTicket, ErreurApiDetaillee, fermerTicket, listerTickets } from "../api/ticketService";
import type { CorpsCreationTicket, StatutTicket, Ticket } from "../types";
import { FiltrageTickets } from "./FiltrageTickets";
import { FormulaireTicket } from "./FormulaireTicket";
import { ListeTickets } from "./ListeTickets";

type EtatDonnees =
  | { kind: "chargement" }
  | { kind: "donnees"; tickets: Ticket[] }
  | { kind: "erreur"; message: string };

export function PageTickets() {
  const [filtre, setFiltre] = useState<string>("");
  const [etat, setEtat] = useState<EtatDonnees>({ kind: "chargement" });
  const [chargement, setChargement] = useState(false);
  const [soumissionEnCours, setSoumissionEnCours] = useState(false);
  const [fermetureEnCoursId, setFermetureEnCoursId] = useState<number | null>(null);

  const [messageSucces, setMessageSucces] = useState<string | null>(null);
  const [messageErreurFormulaire, setMessageErreurFormulaire] = useState<string | null>(null);
  const [messageErreurFermeture, setMessageErreurFermeture] = useState<string | null>(null);

  const charger = useCallback(async (statutSelectionne: string) => {
    setChargement(true);
    setEtat({ kind: "chargement" });
    try {
      const statut = statutSelectionne === "" ? undefined : (statutSelectionne as StatutTicket);
      const tickets = await listerTickets(statut);
      setEtat({ kind: "donnees", tickets });
    } catch (erreur) {
      const message =
        erreur instanceof ErreurApiDetaillee ? erreur.message : "Erreur inattendue lors du chargement.";
      setEtat({ kind: "erreur", message });
    } finally {
      setChargement(false);
    }
  }, []);

  useEffect(() => {
    void charger(filtre);
  }, [charger, filtre]);

  function changerFiltre(nouveau: string) {
    setMessageSucces(null);
    setMessageErreurFermeture(null);
    setFiltre(nouveau);
  }

  async function gererCreation(corps: CorpsCreationTicket) {
    setMessageSucces(null);
    setMessageErreurFormulaire(null);
    setSoumissionEnCours(true);
    try {
      const ticket = await creerTicket(corps);
      setMessageSucces(`Le ticket « ${ticket.titre} » a bien été créé (statut ${ticket.statut}).`);
      await charger(filtre);
    } catch (erreur) {
      if (erreur instanceof ErreurApiDetaillee) {
        setMessageErreurFormulaire(erreur.message);
      } else {
        setMessageErreurFormulaire("Erreur inattendue pendant la création du ticket.");
      }
    } finally {
      setSoumissionEnCours(false);
    }
  }

  async function gererFermeture(ticket: Ticket) {
    setMessageErreurFermeture(null);
    setMessageSucces(null);
    const confirmee = window.confirm(
      `Confirmez-vous la fermeture du ticket « ${ticket.titre} » ? Cette action rend le ticket définitivement fermé.`
    );
    if (!confirmee) {
      return;
    }
    setFermetureEnCoursId(ticket.id);
    try {
      const misAJour = await fermerTicket(ticket.id);
      setMessageSucces(`Le ticket « ${misAJour.titre} » est maintenant FERME.`);
      await charger(filtre);
    } catch (erreur) {
      if (erreur instanceof ErreurApiDetaillee) {
        setMessageErreurFermeture(erreur.message);
      } else {
        setMessageErreurFermeture("Erreur inattendue pendant la fermeture du ticket.");
      }
    } finally {
      setFermetureEnCoursId(null);
    }
  }

  return (
    <main className="page">
      <header className="page__entete">
        <h1>Gestion des tickets de support</h1>
        <p className="page__sous-titre">
          Créez, consultez et fermez vos tickets. Les statuts possibles sont OUVERT, EN_COURS et FERME (RG-03).
        </p>
      </header>

      <section className="carte" aria-label="Création d'un ticket">
        <h2>Créer un ticket</h2>
        <FormulaireTicket soumissionEnCours={soumissionEnCours} onCreer={(c) => void gererCreation(c)} />
        {messageErreurFormulaire ? (
          <div className="alerte alerte--erreur" role="alert">
            {messageErreurFormulaire}
          </div>
        ) : null}
        {messageSucces ? (
          <div className="alerte alerte--succes" role="status">
            {messageSucces}
          </div>
        ) : null}
      </section>

      <section className="carte" aria-label="Liste des tickets">
        <div className="carte__entete">
          <h2>Liste des tickets</h2>
          <FiltrageTickets filtre={filtre} onChangement={changerFiltre} />
        </div>

        {messageErreurFermeture ? (
          <div className="alerte alerte--erreur" role="alert">
            {messageErreurFermeture}
          </div>
        ) : null}

        {chargement ? (
          <div className="etat-chargement" role="status">
            <span className="spinner" aria-hidden="true" /> Chargement des tickets…
          </div>
        ) : etat.kind === "erreur" ? (
          <div className="etat-erreur" role="alert">
            <p>⚠️ {etat.message}</p>
            <button type="button" className="bouton-reessayer" onClick={() => void charger(filtre)}>
              Réessayer
            </button>
          </div>
        ) : etat.kind !== "donnees" || etat.tickets.length === 0 ? (
          <div className="etat-vide">Aucun ticket</div>
        ) : (
          <ListeTickets
            tickets={etat.tickets}
            fermetureEnCoursId={fermetureEnCoursId}
            onFermer={(t) => void gererFermeture(t)}
          />
        )}
      </section>
    </main>
  );
}
