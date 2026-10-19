import type { Ticket } from "../types";
import { BadgeStatut } from "./BadgeStatut";

function formaterDate(valeurIso: string): string {
  const date = new Date(valeurIso);
  return date.toLocaleString("fr-FR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

export function ListeTickets({
  tickets,
  fermetureEnCoursId,
  onFermer,
}: {
  tickets: Ticket[];
  fermetureEnCoursId: number | null;
  onFermer: (ticket: Ticket) => void;
}) {
  return (
    <div className="tableau-responsive">
      <table className="tableau">
        <thead>
          <tr>
            <th>Titre</th>
            <th>Statut</th>
            <th>Date de création</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {tickets.map((ticket) => {
            const ferme = ticket.statut === "FERME";
            const enFermeture = fermetureEnCoursId === ticket.id;
            return (
              <tr key={ticket.id}>
                <td>
                  <span className="tableau__titre">{ticket.titre}</span>
                  {ticket.description ? (
                    <span className="tableau__description">{ticket.description}</span>
                  ) : null}
                </td>
                <td>
                  <BadgeStatut statut={ticket.statut} />
                </td>
                <td>{formaterDate(ticket.dateCreation)}</td>
                <td>
                  {ferme ? (
                    <span className="tableau__muet">Ticket fermé</span>
                  ) : (
                    <button
                      type="button"
                      className="bouton-fermer"
                      onClick={() => onFermer(ticket)}
                      disabled={enFermeture}
                    >
                      {enFermeture ? "Fermeture…" : "Fermer"}
                    </button>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
