import type { CorpsCreationTicket, ErreurApi, StatutTicket, Ticket } from "../types";

/**
 * Service API dedie : TOUTES les communications avec le backend passent ici.
 * Aucun appel HTTP direct dans les composants.
 */
const BASE_URL = "/api/tickets";

function versErreurApi(reponse: Response, texte: string): never {
  let message = "Le serveur a renvoyé une erreur inattendue.";
  try {
    const corps = JSON.parse(texte) as ErreurApi;
    if (corps && typeof corps.message === "string") {
      message = corps.message;
    }
  } catch {
    /* corps non JSON : on garde le message par defaut */
  }
  throw new ErreurApiDetaillee(reponse.status, message);
}

export class ErreurApiDetaillee extends Error {
  readonly statut: number;

  constructor(statut: number, message: string) {
    super(message);
    this.statut = statut;
  }
}

function construireErreurReseau(): ErreurApiDetaillee {
  return new ErreurApiDetaillee(
    0,
    "Le serveur est injoignable. Vérifiez que le backend est démarré, puis réessayez."
  );
}

export async function listerTickets(statut?: StatutTicket): Promise<Ticket[]> {
  const url = statut ? `${BASE_URL}?statut=${encodeURIComponent(statut)}` : BASE_URL;
  let reponse: Response;
  try {
    reponse = await fetch(url, { headers: { Accept: "application/json" } });
  } catch {
    throw construireErreurReseau();
  }
  if (!reponse.ok) {
    await versErreurApi(reponse, await reponse.text());
  }
  return (await reponse.json()) as Ticket[];
}

export async function creerTicket(corps: CorpsCreationTicket): Promise<Ticket> {
  let reponse: Response;
  try {
    reponse = await fetch(BASE_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(corps),
    });
  } catch {
    throw construireErreurReseau();
  }
  if (!reponse.ok) {
    await versErreurApi(reponse, await reponse.text());
  }
  return (await reponse.json()) as Ticket;
}

export async function fermerTicket(id: number): Promise<Ticket> {
  let reponse: Response;
  try {
    reponse = await fetch(`${BASE_URL}/${id}/fermer`, { method: "PATCH" });
  } catch {
    throw construireErreurReseau();
  }
  if (!reponse.ok) {
    await versErreurApi(reponse, await reponse.text());
  }
  return (await reponse.json()) as Ticket;
}
