import { loadStripe, type Stripe } from "@stripe/stripe-js"

/**
 * Stripe.js を 1 度だけロードして使い回す。 publishable key はクライアントに出るので NEXT_PUBLIC_ で公開する。
 */
let stripePromise: Promise<Stripe | null> | null = null

export function getStripe(): Promise<Stripe | null> {
  if (!stripePromise) {
    stripePromise = loadStripe(process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY ?? "")
  }
  return stripePromise
}
