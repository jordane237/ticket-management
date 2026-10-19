import type { StatutTicket } from "../types";

const LIBELLES: Record<StatutTicket, string> = {
  OUVERT: "OUVERT",
  EN_COURS: "EN_COURS",
  FERME: "FERME",
};

export function BadgeStatut({ statut }: { statut: StatutTicket }) {
  return <span className={`badge badge--${statut.toLowerCase()}`}>{LIBELLES[statut]}</span>;
}
