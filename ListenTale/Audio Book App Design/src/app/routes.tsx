import { createBrowserRouter, Outlet } from "react-router";
import { AuthLayout, Welcome, Login, Register, ForgotPassword, Setup } from "./screens/Auth";
import { MainLayout, Books, Search, Library, Challenge, Profile } from "./screens/Main";
import { BookDetail, AudioPlayer, EbookReader, ReviewCreate, ReviewDetail } from "./screens/Media";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: AuthLayout,
    children: [
      { index: true, Component: Welcome },
      { path: "login", Component: Login },
      { path: "register", Component: Register },
      { path: "forgot-password", Component: ForgotPassword },
      { path: "setup", Component: Setup },
    ]
  },
  {
    path: "/app",
    Component: MainLayout,
    children: [
      { index: true, Component: Books },
      { path: "search", Component: Search },
      { path: "library", Component: Library },
      { path: "challenge", Component: Challenge },
      { path: "profile", Component: Profile },
    ]
  },
  {
    path: "/book/:id",
    Component: BookDetail,
  },
  {
    path: "/player/:id",
    Component: AudioPlayer,
  },
  {
    path: "/reader/:id",
    Component: EbookReader,
  },
  {
    path: "/review/create/:id",
    Component: ReviewCreate,
  },
  {
    path: "/review/:id",
    Component: ReviewDetail,
  }
]);
