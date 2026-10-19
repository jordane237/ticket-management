export function FiltrageTickets({
  filtre,
  onChangement,
}: {
  filtre: string;
  onChangement: (nouveauFiltre: string) => void;
}) {
  const choix = [
    { valeur: "", libelle: "Tous" },
    { valeur: "OUVERT", libelle: "OUVERT" },
    { valeur: "EN_COURS", libelle: "EN_COURS" },
    { valeur: "FERME", libelle: "FERME" },
  ];

  return (
    <div className="filtre" role="group" aria-label="Filtrer les tickets par statut">
      {choix.map((c) => (
        <button
          key={c.valeur || "tous"}
          type="button"
          className={`filtre__bouton ${filtre === c.valeur ? "filtre__bouton--actif" : ""}`}
          onClick={() => onChangement(c.valeur)}
        >
          {c.libelle}
        </button>
      ))}
    </div>
  );
}
