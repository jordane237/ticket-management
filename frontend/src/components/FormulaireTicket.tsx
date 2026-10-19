import { useState } from "react";
import type { CorpsCreationTicket } from "../types";

export function FormulaireTicket({
  soumissionEnCours,
  onCreer,
}: {
  soumissionEnCours: boolean;
  onCreer: (corps: CorpsCreationTicket) => void;
}) {
  const [titre, setTitre] = useState("");
  const [description, setDescription] = useState("");

  const titrenomValide = titre.trim().length > 0;

  function soumettre(evenement: React.FormEvent) {
    evenement.preventDefault();
    if (!titrenomValide || soumissionEnCours) {
      return;
    }
    const corps: CorpsCreationTicket = { titre: titre.trim() };
    const descriptionNettoyee = description.trim();
    if (descriptionNettoyee.length > 0) {
      corps.description = descriptionNettoyee;
    }
    onCreer(corps);
    setTitre("");
    setDescription("");
  }

  return (
    <form className="formulaire" onSubmit={soumettre}>
      <label htmlFor="titre">Titre du ticket</label>
      <input
        id="titre"
        name="titre"
        type="text"
        placeholder="Ex. : Problème de connexion"
        value={titre}
        onChange={(e) => setTitre(e.target.value)}
        disabled={soumissionEnCours}
      />
      <label htmlFor="description">Description (facultative)</label>
      <textarea
        id="description"
        name="description"
        rows={3}
        placeholder="Ex. : Impossible de se connecter au service"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        disabled={soumissionEnCours}
      />
      <button type="submit" disabled={!titrenomValide || soumissionEnCours}>
        {soumissionEnCours ? "Création en cours…" : "Créer le ticket"}
      </button>
    </form>
  );
}
